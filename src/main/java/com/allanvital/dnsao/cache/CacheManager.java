package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.cache.map.LruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.RewarmCacheManager;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.util.Collections;
import java.util.Map;

import static com.allanvital.dnsao.AppLoggers.CACHE;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheManager {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final boolean enabled;
    private final Map<String, DnsCacheEntry> cache;
    private final RewarmCacheManager rewarmCacheManager;

    private final NotificationManager notificationManager = NotificationManager.getInstance();

    public CacheManager(CacheConf cacheConf, RewarmCacheManager rewarmCacheManager) {
        if (cacheConf == null || !cacheConf.isEnabled()) {
            enabled = false;
            cache = null;
            this.rewarmCacheManager = null;
            return;
        }
        enabled = true;
        this.rewarmCacheManager = rewarmCacheManager;
        this.cache = Collections.synchronizedMap(new LruDnsCache(cacheConf.getMaxCacheEntries()));
    }

    public Message get(String key) {
        if (!enabled) {
            return null;
        }
        DnsCacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.info("cache hit for {}", key);
            notificationManager.notify(EventType.CACHE_HIT);
            return entry.getResponse();
        }

        DnsCacheEntry warmEntry = this.rewarmCacheManager.take(key);
        if (warmEntry != null && !warmEntry.isExpired()) {
            log.info("warm cache hit for {}", key);
            this.promote(key, warmEntry);
            notificationManager.notify(EventType.CACHE_HIT);
            return warmEntry.getResponse();
        }

        if (entry != null && entry.isExpired()) {
            log.info("cache entry {} was found, but expired. Removing", key);
            cache.remove(key);
            return null;
        }

        return null;
    }

    private void promote(String key, DnsCacheEntry entry) {
        if (!enabled) {
            return;
        }
        log.info("promoting {} to cache", key);
        entry.setRewarmCount(0);
        addEntry(key, entry);
        notificationManager.notify(EventType.CACHE_REWARM_PROMOTION);
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
        rewarmCacheManager.schedule(key, entry);
    }

}