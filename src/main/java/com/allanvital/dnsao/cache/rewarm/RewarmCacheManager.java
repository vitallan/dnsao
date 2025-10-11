package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.map.LruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.utils.ThreadShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;

import static com.allanvital.dnsao.AppLoggers.CACHE;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmCacheManager {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final long thresholdMs;
    private final PriorityBlockingQueue<RewarmTask> queue = new PriorityBlockingQueue<>(
            1024, Comparator.comparingLong(RewarmTask::getTriggerAtMs)
    );

    //using this as index to avoid removal from PriorityQueue directly for performance reasons
    private final ConcurrentHashMap<String, RewarmTask> index = new ConcurrentHashMap<>();
    private final Map<String, DnsCacheEntry> cache;
    private final ExecutorService executor;
    private final RewarmWorker worker;
    private volatile boolean running = false;
    private final boolean enabled;

    public RewarmCacheManager(CacheConf cacheConf, ResolverFactory resolverFactory, long thresholdMs) {
        if (cacheConf == null || !cacheConf.isRewarm()) {
            this.enabled = false;
            this.thresholdMs = thresholdMs;
            this.cache = null;
            this.executor = null;
            this.worker = null;
            return;
        }
        this.enabled = true;
        this.thresholdMs = thresholdMs;
        this.cache = Collections.synchronizedMap(new LruDnsCache(cacheConf.getMaxCacheEntries()));
        this.running = true;
        this.executor = ThreadShop.buildExecutor("rewarm", 1);
        this.worker = new RewarmWorker(queue, index, resolverFactory, this, cacheConf.getMaxRewarmCount());
        this.executor.submit(worker::loop);
    }

    private DnsCacheEntry get(String key) {
        if (!enabled) {
            return null;
        }
        log.debug("trying to find {} in rewarm cache", key);
        if (!cache.containsKey(key)) {
            log.debug("key {} not found in rewarm cache", key);
            return null;
        }
        DnsCacheEntry entry = cache.get(key);
        log.debug("key {} found in rewarm cache", key);
        return entry;
    }

    public void put(String key, DnsCacheEntry dnsCacheEntry) {
        log.debug("adding {} to rewarm cache", key);
        this.cache.put(key, dnsCacheEntry);
    }

    public DnsCacheEntry take(String key) {
        if (!enabled) {
            return null;
        }
        DnsCacheEntry entry = this.get(key);
        cache.remove(key);
        this.cancel(key);
        log.debug("removed key {} from rewarm cache to promotion", key);
        return entry;
    }

    public void schedule(String key, DnsCacheEntry entry) {
        if (!enabled) {
            return;
        }
        log.debug("scheduling key {} for rewarm", key);
        long expiryTime = entry.getExpiryTime();
        long now = System.currentTimeMillis();
        long triggerAt = Math.max(now, expiryTime - thresholdMs);
        RewarmTask newTask = new RewarmTask(key, triggerAt, entry);
        index.put(key, newTask);
        queue.offer(newTask);
    }

    public void cancel(String key) {
        if (!enabled) {
            return;
        }
        log.debug("canceling schedule for key {} from rewarm", key);
        index.remove(key);
        this.cache.remove(key);
    }

    public boolean isEmpty() {
        return cache.isEmpty() && index.isEmpty();
    }

    public void shutdown() {
        running = false;
        if (worker != null) {
            worker.shutdown();
        }
        this.executor.shutdown();
    }

    public String toString() {
        return this.cache + ":" + this.index;
    }

}