package com.allanvital.dnsao.cache;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.map.KeepAwareLruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.xbill.DNS.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_ADDED;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REMOVED;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheManager {


    private final boolean enabled;
    private final Map<String, DnsCacheEntry> cache;
    private final CacheStats cacheStats;

    private final FixedTimeRewarmScheduler fixedTimeRewarmScheduler;
    private final ExpiredConf expiredConf;

    public CacheManager(CacheConf cacheConf,
                        FixedTimeRewarmScheduler fixedTimeRewarmScheduler,
                        ExpiredConf expiredConf,
                        KeepProvider keepProvider) {
        if (cacheConf == null || !cacheConf.isEnabled()) {
            enabled = false;
            cache = null;
            cacheStats = null;
            this.fixedTimeRewarmScheduler = null;
            this.expiredConf = null;
            return;
        }
        this.expiredConf = expiredConf;
        enabled = true;
        this.fixedTimeRewarmScheduler = fixedTimeRewarmScheduler;

        int maxEntries = cacheConf.getMaxCacheEntries();
        KeepAwareLruDnsCache keepAwareLruDnsCache = new KeepAwareLruDnsCache(maxEntries, keepProvider);
        this.cache = Collections.synchronizedMap(keepAwareLruDnsCache);
        this.cacheStats = keepAwareLruDnsCache;
    }

    public CacheStats getCacheStats() {
        return cacheStats;
    }

    public DnsCacheEntry safeGet(String key) {
        return cache.get(key);
    }

    public DnsCacheEntry getStale(String key) {
        if (!enabled || !expiredConf.isServeExpired()) {
            return null;
        }
        DnsCacheEntry entry = safeGet(key);
        if (entry != null) {
            if (!entry.isExpired(expiredConf.getServeExpiredMax())) {
                Log.CACHE.info("stale cache hit for {}", key);
                telemetryNotify(EventType.STALE_CACHE_HIT);
                return entry;
            }
            if (shouldRemove(entry)) {
                Log.CACHE.info("cache entry {} was found, but expired. Removing", key);
                remove(key);
            }
        }
        return null;
    }

    public DnsCacheEntry get(String key) {
        if (!enabled) {
            return null;
        }
        DnsCacheEntry entry = safeGet(key);
        if (entry != null && !entry.isStale()) {
            Log.CACHE.info("cache hit for {}", key);
            entry.setRewarmCount(0);
            cache.put(key, entry);
            telemetryNotify(EventType.CACHE_HIT);
            return entry;
        }

        if (entry != null && entry.isStale()) {
            Log.CACHE.info("cache entry {} was found, but stale ", key);
            if (shouldRemove(entry)) {
                remove(key);
            }
            return null;
        }

        return null;
    }

    public void rewarm(String key, DnsCacheEntry entry) {
        if (!enabled) {
            return;
        }
        Log.CACHE.debug("rewarming entry {}", key);
        addEntry(key, entry);
    }

    public void put(String key, Message response, Long ttlSecs) {
        if (!enabled) {
            return;
        }
        Log.CACHE.info("adding {} to cache", key);
        addEntry(key, new DnsCacheEntry(response, ttlSecs));
    }

    public void purgeExpired() {
        if (!enabled) {
            return;
        }
        List<String> keys;
        synchronized (cache) {
            keys = new ArrayList<>(cache.keySet());
        }
        for (String key : keys) {
            DnsCacheEntry entry = cache.get(key);
            if (entry != null && shouldRemove(entry)) {
                remove(key);
            }
        }
    }

    private void remove(String key) {
        cache.remove(key);
        telemetryNotify(CACHE_REMOVED);
    }

    private boolean shouldRemove(DnsCacheEntry entry) {
        if (!entry.isStale()) {
            return false;
        }
        if (expiredConf.isServeExpired() && !entry.isExpired(expiredConf.getServeExpiredMax())) {
            return false;
        }
        return true;
    }

    private void addEntry(String key, DnsCacheEntry entry) {
        cache.put(key, entry);
        telemetryNotify(CACHE_ADDED);
        fixedTimeRewarmScheduler.schedule(key, entry.getExpiryTime());
    }

}
