package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.remote.QueryProcessor;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.allanvital.dnsao.AppLoggers.CACHE;
import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static com.allanvital.dnsao.dns.remote.DnsUtils.isWarmable;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final RewarmScheduler scheduler;
    private final CacheManager cache;
    private final QueryProcessorFactory queryProcessorFactory;
    private final int maxRewarmCount;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private long lastBeat = System.currentTimeMillis();

    private final NotificationManager notificationManager = NotificationManager.getInstance();

    public RewarmWorker(RewarmScheduler scheduler, CacheManager cache, QueryProcessorFactory queryProcessorFactory, int maxRewarmCount) {
        this.scheduler = scheduler;
        this.cache = cache;
        this.queryProcessorFactory = queryProcessorFactory;
        this.maxRewarmCount = maxRewarmCount;
        log.debug("starting RewarWorker");
    }

    public void shutdown() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    private void heartbeat() {
        long now = System.currentTimeMillis();
        if (now - lastBeat >= 30_000) {
            int qsize = scheduler.queue().size();
            log.debug("heartbeat: queueSize={}", qsize);
            lastBeat = now;
        }
    }

    @Override
    public void run() {
        String key = "";
        while (running.get()) {
            heartbeat();
            try {
                RewarmTask task = scheduler.queue().poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                key = task.getKey();
                DnsCacheEntry entry = cache.safeGet(key);
                if (entry == null) {
                    log.debug("rewarm was scheduled but key already left cache key={}", key);
                    continue;
                }

                int currentRewarmCount = entry.getRewarmCount();
                if (currentRewarmCount > maxRewarmCount) { //better to remove scheduled afterward to ensure cache is correctly clean
                    log.debug("max rewarm count for key={}", key);
                    notificationManager.notify(EventType.CACHE_REWARM_EXPIRED);
                    continue;
                }
                Message cachedResponse = entry.getResponse();
                Record question = cachedResponse.getQuestion();
                if (question == null) {
                    log.debug("rewarm skip: missing QUESTION section for key={}", key);
                    continue;
                }
                if (!isWarmable(cachedResponse)) {
                    log.debug("rewarm skip: not warmable (rcode/answers) key={}", key);
                    continue;
                }
                Message query = Message.newQuery(question);
                QueryProcessor queryProcessor = queryProcessorFactory.buildCacheLessQueryProcessor();
                DnsQuery queryResponse = queryProcessor.processQuery(InetAddress.getByName("127.0.0.1"), query.toWire());
                Message newResponse = queryResponse.getResponse();
                if (newResponse == null) {
                    log.debug("it was not possible to rewarm entry {}", key);
                    continue;
                }
                Long ttlFromDirectResponse = getTtlFromDirectResponse(newResponse);

                if (ttlFromDirectResponse == null) {
                    log.debug("rewarm skip (upstream result not warmable) key={} rcode={}",
                            key, Rcode.string(newResponse.getRcode()));
                    continue;
                }

                DnsCacheEntry updateCacheEntry = new DnsCacheEntry(newResponse, ttlFromDirectResponse, currentRewarmCount + 1);
                cache.rewarm(key, updateCacheEntry);
                notificationManager.notify(EventType.CACHE_REWARM);
                log.debug("rewarm stored (warm cache) key={} ttl={}s qname={} type={}",
                        key, ttlFromDirectResponse,
                        question.getName().toString(),
                        Type.string(question.getType()));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                running.set(false);
                log.debug("stopping rewarm worker");
                break;
            } catch (Throwable t) {
                log.debug("Error on key '{}' rewarmWorker: {}", key, t.getMessage());
                if (log.isDebugEnabled()) {
                    t.printStackTrace();
                }
            }
        }
    }
}
