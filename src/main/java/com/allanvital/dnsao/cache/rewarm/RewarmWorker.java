package com.allanvital.dnsao.cache.rewarm;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmWorker implements Runnable {

    private static final int MAX_TRANSIENT_REWARM_RETRIES = 1;

    private final FixedTimeRewarmScheduler scheduler;
    private final CacheManager cache;
    private final QueryProcessorFactory queryProcessorFactory;
    private final int maxRewarmCount;
    private final RewarmRetryPolicy retryPolicy;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private long lastBeat = Clock.currentTimeInMillis();

    public RewarmWorker(FixedTimeRewarmScheduler scheduler,
                        CacheManager cache,
                        QueryProcessorFactory queryProcessorFactory,
                        int maxRewarmCount) {

        this.scheduler = scheduler;
        this.cache = cache;
        this.queryProcessorFactory = queryProcessorFactory;
        this.maxRewarmCount = maxRewarmCount;
        this.retryPolicy = new RewarmRetryPolicy(MAX_TRANSIENT_REWARM_RETRIES);
        Log.CACHE.debug("starting RewarmWorker");
    }

    public void shutdown() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    private void heartbeat() {
        long now = Clock.currentTimeInMillis();
        if (now - lastBeat >= 30_000) {
            int qsize = scheduler.queue().size();
            Log.CACHE.debug("heartbeat: queueSize={}", qsize);
            lastBeat = now;
        }
    }

    @Override
    public void run() {
        while (running.get()) {
            heartbeat();
            try {
                RewarmTask task = scheduler.queue().poll(50, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                processTask(task);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running.set(false);
                Log.CACHE.debug("stopping rewarm worker");
                break;
            } catch (Throwable t) {
                Log.CACHE.debug("Error on rewarm worker loop: {}", t.getMessage(), t);
            }
        }
    }

    private void processTask(RewarmTask task) {
        String key = task.getKey();
        RewarmContext context = loadContext(task);
        RewarmSkipReason skipReason = validateBeforeAttempt(context);
        if (skipReason != null) {
            recordSkip(key, skipReason, context);
            return;
        }

        Log.CACHE.debug("starting rewarm of key={} on currentRewarmCount={}", key, context.currentRewarmCount());
        RewarmAttemptResult attemptResult = attemptRewarmWithRetry(context);
        if (!attemptResult.success()) {
            recordFailure(key, attemptResult);
            return;
        }

        Long ttlFromDirectResponse = getTtlFromDirectResponse(attemptResult.response());
        if (ttlFromDirectResponse == null) {
            telemetryNotify(EventType.CACHE_REWARM_NO_TTL);
            Log.CACHE.debug("rewarm skip (upstream result not warmable) key={} rcode={}",
                    key, Rcode.string(attemptResult.response().getRcode()));
            return;
        }

        RewarmSkipReason storeSkipReason = validateBeforeStore(context);
        if (storeSkipReason != null) {
            recordSkip(key, storeSkipReason, context);
            return;
        }

        storeRewarmedEntry(context, attemptResult.response(), ttlFromDirectResponse);
        recordSuccess(context, ttlFromDirectResponse);
    }

    private boolean isWarmable(Message msg) {
        return getTtlFromDirectResponse(msg) != null;
    }

    private RewarmContext loadContext(RewarmTask task) {
        String key = task.getKey();
        DnsCacheEntry entry = cache.safeGet(key);
        if (entry == null) {
            return new RewarmContext(key, task, null, null, 0, false);
        }
        Message cachedResponse = entry.getResponse();
        Record question = cachedResponse != null ? cachedResponse.getQuestion() : null;
        boolean shouldAlwaysRewarm = cache.shouldAlwaysRewarm(key, question);
        return new RewarmContext(key, task, entry, question, entry.getRewarmCount(), shouldAlwaysRewarm);
    }

    private RewarmSkipReason validateBeforeAttempt(RewarmContext context) {
        if (context == null || context.entry() == null) {
            return RewarmSkipReason.MISSING_ENTRY;
        }
        if (isTaskObsolete(context.task(), context.entry())) {
            return RewarmSkipReason.OBSOLETE_TASK;
        }
        if (context.question() == null) {
            return RewarmSkipReason.MISSING_QUESTION;
        }
        if (!isWarmable(context.entry().getResponse())) {
            return RewarmSkipReason.NOT_WARMABLE_CACHED_RESPONSE;
        }
        if (context.currentRewarmCount() >= maxRewarmCount && !context.shouldAlwaysRewarm()) {
            telemetryNotify(EventType.CACHE_REWARM_EXPIRED);
            return RewarmSkipReason.MAX_REWARM_REACHED;
        }
        return null;
    }

    private RewarmAttemptResult attemptRewarmWithRetry(RewarmContext context) {
        Message query = Message.newQuery(context.question());
        int attempts = 0;
        while (true) {
            RewarmAttemptResult result = attemptRewarmOnce(query, context.key());
            if (result.success()) {
                return result;
            }
            if (!retryPolicy.shouldRetry(attempts, result)) {
                return result;
            }
            attempts++;
        }
    }

    private RewarmAttemptResult attemptRewarmOnce(Message query, String key) {
        try {
            QueryProcessor queryProcessor = queryProcessorFactory.buildQueryProcessor();
            DnsQuery queryResponse = queryProcessor.processInternalQuery(query);
            if (queryResponse == null || queryResponse.getResponse() == null) {
                Log.CACHE.debug("rewarm returned no response on key={}, retrying", key);
                return RewarmAttemptResult.retryableFailure("no_response");
            }
            Message response = queryResponse.getResponse();
            if (getTtlFromDirectResponse(response) != null) {
                return RewarmAttemptResult.success(response);
            }
            if (response.getRcode() == Rcode.SERVFAIL) {
                Log.CACHE.debug("rewarm returned SERVFAIL on key={}, retrying", key);
                return RewarmAttemptResult.retryableFailure("servfail");
            }
            return RewarmAttemptResult.terminalFailure("not_warmable_response");
        } catch (Throwable t) {
            Log.CACHE.debug("rewarm transient failure on key={} message={}", key, t.getMessage());
            return RewarmAttemptResult.retryableFailure("exception");
        }
    }

    private RewarmSkipReason validateBeforeStore(RewarmContext context) {
        DnsCacheEntry latestEntry = cache.safeGet(context.key());
        if (latestEntry == null) {
            return RewarmSkipReason.REMOVED_BEFORE_STORE;
        }
        if (isTaskObsolete(context.task(), latestEntry)) {
            return RewarmSkipReason.REPLACED_BEFORE_STORE;
        }
        return null;
    }

    private void storeRewarmedEntry(RewarmContext context, Message response, long ttlFromDirectResponse) {
        DnsCacheEntry updateCacheEntry = new DnsCacheEntry(response, ttlFromDirectResponse, context.currentRewarmCount() + 1);
        cache.rewarm(context.key(), updateCacheEntry);
    }

    private boolean isTaskObsolete(RewarmTask task, DnsCacheEntry entry) {
        return task.getExpectedExpiryTimeMs() != -1L && entry.getExpiryTime() != task.getExpectedExpiryTimeMs();
    }

    private void recordSkip(String key, RewarmSkipReason reason, RewarmContext context) {
        long expectedExpiry = context != null && context.task() != null ? context.task().getExpectedExpiryTimeMs() : -1L;
        long currentExpiry = context != null && context.entry() != null ? context.entry().getExpiryTime() : -1L;
        if (reason == RewarmSkipReason.REPLACED_BEFORE_STORE) {
            DnsCacheEntry latestEntry = cache.safeGet(key);
            currentExpiry = latestEntry != null ? latestEntry.getExpiryTime() : -1L;
        }
        Log.CACHE.debug(RewarmSkipReason.MESSAGE, key, reason.reason(), expectedExpiry, currentExpiry);
    }

    private void recordFailure(String key, RewarmAttemptResult result) {
        telemetryNotify(EventType.CACHE_REWARM_FAILED);
        Log.CACHE.debug("it was not possible to rewarm entry {} reason={}", key, result.failureReason());
    }

    private void recordSuccess(RewarmContext context, long ttlFromDirectResponse) {
        telemetryNotify(EventType.CACHE_REWARM);
        Log.CACHE.debug("rewarm stored (warm cache) key={} ttl={}s qname={} type={}",
                context.key(), ttlFromDirectResponse,
                context.question().getName().toString(),
                Type.string(context.question().getType()));
    }

}
