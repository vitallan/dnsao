package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.log.Log;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

public class RewarmCoordinator implements Runnable {

    private static final long PERMIT_ACQUIRE_TIMEOUT_MS = 10L;
    private static final long SATURATION_REQUEUE_DELAY_MS = 25L;

    private final FixedTimeRewarmScheduler scheduler;
    private final CacheManager cache;
    private final QueryProcessorFactory queryProcessorFactory;
    private final int maxRewarmCount;
    private final ExecutorService executionPool;
    private final Semaphore workerPermits;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private long lastBeat = Clock.currentTimeInMillis();

    public RewarmCoordinator(FixedTimeRewarmScheduler scheduler,
                             CacheManager cache,
                             QueryProcessorFactory queryProcessorFactory,
                             int maxRewarmCount,
                             ExecutorService executionPool,
                             int workerPoolSize) {
        this.scheduler = scheduler;
        this.cache = cache;
        this.queryProcessorFactory = queryProcessorFactory;
        this.maxRewarmCount = maxRewarmCount;
        this.executionPool = executionPool;
        this.workerPermits = new Semaphore(Math.max(1, workerPoolSize));
        Log.CACHE.debug("starting RewarmCoordinator with workerPoolSize={}", Math.max(1, workerPoolSize));
    }

    public void shutdown() {
        running.set(false);
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
                coordinate(task);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running.set(false);
                Log.CACHE.debug("stopping rewarm coordinator");
                break;
            } catch (Throwable t) {
                Log.CACHE.debug("Error on rewarm coordinator loop: {}", t.getMessage(), t);
            }
        }
    }

    private void coordinate(RewarmTask task) throws InterruptedException {
        String key = task.getKey();
        RewarmContext context = loadContext(task);
        RewarmSkipReason skipReason = validateBeforeDispatch(context);
        if (skipReason != null) {
            recordSkip(key, skipReason, context);
            return;
        }

        if (!workerPermits.tryAcquire(PERMIT_ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            scheduler.scheduleSoon(key, task.getExpectedExpiryTimeMs(), SATURATION_REQUEUE_DELAY_MS);
            Log.CACHE.debug("rewarm saturation: requeueing key={} expectedExpiry={}", key, task.getExpectedExpiryTimeMs());
            return;
        }

        executionPool.submit(new RewarmWorker(cache, queryProcessorFactory, context, workerPermits));
    }

    private void heartbeat() {
        long now = Clock.currentTimeInMillis();
        if (now - lastBeat >= 30_000) {
            Log.CACHE.debug("heartbeat: queueSize={} availableWorkers={}", scheduler.queue().size(), workerPermits.availablePermits());
            lastBeat = now;
        }
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

    private RewarmSkipReason validateBeforeDispatch(RewarmContext context) {
        if (context == null || context.entry() == null) {
            return RewarmSkipReason.MISSING_ENTRY;
        }
        if (isTaskObsolete(context.task(), context.entry())) {
            return RewarmSkipReason.OBSOLETE_TASK;
        }
        if (context.question() == null) {
            return RewarmSkipReason.MISSING_QUESTION;
        }
        if (!isWarmable(context.entry().getResponse()) && !context.shouldAlwaysRewarm()) {
            return RewarmSkipReason.NOT_WARMABLE_CACHED_RESPONSE;
        }
        if (context.currentRewarmCount() >= maxRewarmCount && !context.shouldAlwaysRewarm()) {
            telemetryNotify(EventType.CACHE_REWARM_EXPIRED);
            return RewarmSkipReason.MAX_REWARM_REACHED;
        }
        return null;
    }

    private boolean isWarmable(Message msg) {
        return getTtlFromDirectResponse(msg) != null;
    }

    private boolean isTaskObsolete(RewarmTask task, DnsCacheEntry entry) {
        return task.getExpectedExpiryTimeMs() != -1L && entry.getExpiryTime() != task.getExpectedExpiryTimeMs();
    }

    private void recordSkip(String key, RewarmSkipReason reason, RewarmContext context) {
        long expectedExpiry = context != null && context.task() != null ? context.task().getExpectedExpiryTimeMs() : -1L;
        long currentExpiry = context != null && context.entry() != null ? context.entry().getExpiryTime() : -1L;
        Log.CACHE.debug(RewarmSkipReason.MESSAGE, key, reason.reason(), expectedExpiry, currentExpiry);
    }
}
