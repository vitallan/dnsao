package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.cache.map.LruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.util.Collections;
import java.util.Map;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_ADDED;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheManager {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final boolean enabled;
    private final Map<String, DnsCacheEntry> cache;

    private final FixedTimeRewarmScheduler fixedTimeRewarmScheduler;
    private final ExpiredConf expiredConf;

    public CacheManager(CacheConf cacheConf, FixedTimeRewarmScheduler fixedTimeRewarmScheduler, ExpiredConf expiredConf) {
        if (cacheConf == null || !cacheConf.isEnabled()) {
            enabled = false;
            cache = null;
            this.fixedTimeRewarmScheduler = null;
            this.expiredConf = null;
            return;
        }
        this.expiredConf = expiredConf;
        enabled = true;
        this.fixedTimeRewarmScheduler = fixedTimeRewarmScheduler;
        this.cache = Collections.synchronizedMap(new LruDnsCache(cacheConf.getMaxCacheEntries()));
    }

    public DnsCacheEntry safeGet(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        return null;
    }

    public DnsCacheEntry getStale(String key) {
        if (!enabled || !expiredConf.isServeExpired()) {
            return null;
        }
        DnsCacheEntry entry = safeGet(key);
        if (entry != null) {
            if (!entry.isExpired(expiredConf.getServeExpiredMax())) {
                log.info("stale cache hit for {}", key);
                telemetryNotify(EventType.STALE_CACHE_HIT);
                return entry;
            }
            log.info("cache entry {} was found, but expired. Removing", key);
            cache.remove(key);
        }
        return null;
    }

    public DnsCacheEntry get(String key) {
        if (!enabled) {
            return null;
        }
        DnsCacheEntry entry = safeGet(key);
        if (entry != null && !entry.isStale()) {
            log.info("cache hit for {}", key);
            entry.setRewarmCount(0);
            cache.put(key, entry);
            telemetryNotify(EventType.CACHE_HIT);
            return entry;
        }

        if (entry != null && entry.isStale()) {
            log.info("cache entry {} was found, but stale ", key);
            if (!expiredConf.isServeExpired()) {
                cache.remove(key);
            }
            if (expiredConf.isServeExpired() && entry.isExpired(expiredConf.getServeExpiredMax())) {
                log.debug("cache entry {} was found, but stale and expired ", key);
                cache.remove(key);
            }
            return null;
        }

        return null;
    }

    public void rewarm(String key, DnsCacheEntry entry) {
        if (!enabled) {
            return;
        }
        log.debug("rewarming entry {}", key);
        addEntry(key, entry);
    }

    public void put(String key, Message response, Long ttlSecs) {
        if (!enabled) {
            return;
        }
        log.info("adding {} to cache", key);
        addEntry(key, new DnsCacheEntry(response, ttlSecs));
    }

    private void addEntry(String key, DnsCacheEntry entry) {
        cache.put(key, entry);
        telemetryNotify(CACHE_ADDED);
        fixedTimeRewarmScheduler.schedule(key, entry.getExpiryTime());
    }

}