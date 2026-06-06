package com.allanvital.dnsao.cache.rewarm;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
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


    private final FixedTimeRewarmScheduler scheduler;
    private final CacheManager cache;
    private final QueryProcessorFactory queryProcessorFactory;
    private final KeepProvider keepProvider;
    private final int maxRewarmCount;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private long lastBeat = Clock.currentTimeInMillis();

    public RewarmWorker(FixedTimeRewarmScheduler scheduler,
                        CacheManager cache,
                        QueryProcessorFactory queryProcessorFactory,
                        KeepProvider keepProvider,
                        int maxRewarmCount) {

        this.scheduler = scheduler;
        this.cache = cache;
        this.queryProcessorFactory = queryProcessorFactory;
        this.keepProvider = keepProvider;
        this.maxRewarmCount = maxRewarmCount;
        Log.CACHE.debug("starting RewarWorker");
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
        String key = "";
        while (running.get()) {
            heartbeat();
            try {
                RewarmTask task = scheduler.queue().poll(50, TimeUnit.MILLISECONDS);
                if (task == null) {
                    continue;
                }
                key = task.getKey();
                DnsCacheEntry entry = cache.safeGet(key);
                if (entry == null) {
                    Log.CACHE.debug("rewarm was scheduled but key already left cache key={}", key);
                    continue;
                }

                Message cachedResponse = entry.getResponse();
                Record question = cachedResponse.getQuestion();
                if (question == null) {
                    Log.CACHE.debug("rewarm skip: missing QUESTION section for key={}", key);
                    continue;
                }
                if (!isWarmable(cachedResponse)) {
                    Log.CACHE.debug("rewarm skip: not warmable (rcode/answers) key={}", key);
                    continue;
                }
                int currentRewarmCount = entry.getRewarmCount();
                boolean isInCacheKeep = keepProvider.contain(question);
                if (currentRewarmCount >= maxRewarmCount && !isInCacheKeep) { //better to remove scheduled afterward to ensure cache is correctly clean
                    Log.CACHE.debug("max rewarm count for key={}", key);
                    telemetryNotify(EventType.CACHE_REWARM_EXPIRED);
                    continue;
                }
                Log.CACHE.debug("starting rewarm of key={} on currentRewarmCount={}", key, currentRewarmCount);
                Message query = Message.newQuery(question);
                QueryProcessor queryProcessor = queryProcessorFactory.buildQueryProcessor();
                DnsQuery queryResponse = queryProcessor.processInternalQuery(query);
                Message newResponse = queryResponse.getResponse();
                if (newResponse == null) {
                    telemetryNotify(EventType.CACHE_REWARM_FAILED);
                    Log.CACHE.debug("it was not possible to rewarm entry {}", key);
                    continue;
                }
                Long ttlFromDirectResponse = getTtlFromDirectResponse(newResponse);

                if (ttlFromDirectResponse == null) {
                    telemetryNotify(EventType.CACHE_REWARM_NO_TTL);
                    Log.CACHE.debug("rewarm skip (upstream result not warmable) key={} rcode={}",
                            key, Rcode.string(newResponse.getRcode()));
                    continue;
                }
                DnsCacheEntry updateCacheEntry = new DnsCacheEntry(newResponse, ttlFromDirectResponse, currentRewarmCount + 1);
                cache.rewarm(key, updateCacheEntry);
                telemetryNotify(EventType.CACHE_REWARM);
                Log.CACHE.debug("rewarm stored (warm cache) key={} ttl={}s qname={} type={}",
                        key, ttlFromDirectResponse,
                        question.getName().toString(),
                        Type.string(question.getType()));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running.set(false);
                Log.CACHE.debug("stopping rewarm worker");
                break;
            } catch (Throwable t) {
                Log.CACHE.debug("Error on key '{}' rewarmWorker: {}", key, t.getMessage(), t);
            }
        }
    }

    private boolean isWarmable(Message msg) {
        return getTtlFromDirectResponse(msg) != null;
    }

}
