package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import static com.allanvital.dnsao.AppLoggers.CACHE;
import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static com.allanvital.dnsao.dns.remote.DnsUtils.isWarmable;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmWorker {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final PriorityBlockingQueue<RewarmTask> queue;
    private final ConcurrentHashMap<String, RewarmTask> index;
    private final ResolverFactory resolverFactory;
    private final RewarmCacheManager cacheManager;
    private boolean running = false;
    private int maxRewarmCount = 1;

    private final NotificationManager notificationManager = NotificationManager.getInstance();

    public RewarmWorker(PriorityBlockingQueue<RewarmTask> queue, ConcurrentHashMap<String, RewarmTask> index,
                        ResolverFactory resolverFactory, RewarmCacheManager cacheManager,
                        int maxRewarmCount) {
        this.queue = queue;
        this.index = index;
        this.maxRewarmCount = maxRewarmCount;
        this.resolverFactory = resolverFactory;
        this.cacheManager = cacheManager;
        running = true;
    }

    public void loop() {
        while (running) {
            try {
                RewarmTask head = queue.peek();
                if (head == null) {
                    Thread.sleep(100);
                    continue;
                }
                long now = System.currentTimeMillis();
                long wait = head.getTriggerAtMs() - now;
                if (wait > 0) {
                    Thread.sleep(Math.min(wait, 1000));
                    continue;
                }

                RewarmTask task = queue.poll();
                if (task == null) continue;

                RewarmTask latest = index.get(task.getKey());
                if (latest != task) {
                    continue;
                }
                index.remove(task.getKey());
                try {
                    NamedResolver resolver = resolverFactory.getResolver();
                    DnsCacheEntry dnsCacheEntry = task.getDnsCacheEntry();
                    warm(task.getKey(), resolver, dnsCacheEntry);
                } catch (Throwable t) {
                    Throwable rootCause = ExceptionUtils.findRootCause(t);
                    log.warn("failed at rewarmWorker: {}", rootCause.getMessage());
                }

            } catch (InterruptedException ie) {
                log.debug("stopping rewarm worker");
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                Throwable rootCause = ExceptionUtils.findRootCause(t);
                log.warn("failed at rewarmWorker: {}", rootCause.getMessage());
            }
        }
    }

    private void warm(String key, NamedResolver resolver, DnsCacheEntry dnsCacheEntry) throws IOException {
        int currentRewarmCount = dnsCacheEntry.getRewarmCount();
        if (currentRewarmCount >= maxRewarmCount) { //better to remove scheduled afterward to ensure warmCache is correctly clean
            log.debug("max rewarm count for key={}", key);
            cacheManager.cancel(key);
            cacheManager.take(key);
            notificationManager.notify(EventType.CACHE_REWARM_EXPIRED);
            return;
        }
        Message cachedResponse = dnsCacheEntry.getResponse();
        Record question = cachedResponse.getQuestion();
        if (question == null) {
            log.debug("rewarm skip: missing QUESTION section for key={}", key);
            return;
        }
        if (!isWarmable(cachedResponse)) {
            log.debug("rewarm skip: not warmable (rcode/answers) key={}", key);
            return;
        }
        Message query = Message.newQuery(question);
        Message newResponse = resolver.send(query);
        Long ttlFromDirectResponse = getTtlFromDirectResponse(newResponse);
        if (ttlFromDirectResponse == null) {
            log.debug("rewarm skip (upstream result not warmable) key={} rcode={}",
                    key, Rcode.string(newResponse.getRcode()));
            return;
        }
        DnsCacheEntry updateCacheEntry = new DnsCacheEntry(newResponse, ttlFromDirectResponse, currentRewarmCount + 1);
        cacheManager.put(key, updateCacheEntry);
        cacheManager.schedule(key, updateCacheEntry);
        notificationManager.notify(EventType.CACHE_REWARM);
        log.info("rewarm stored (warm cache) key={} ttl={}s qname={} type={}",
                key, ttlFromDirectResponse,
                question.getName().toString(),
                Type.string(question.getType()));

    }

    public void shutdown() {
        running = false;
    }

}