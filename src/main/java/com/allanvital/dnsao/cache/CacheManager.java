package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.cache.map.LruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.RewarmScheduler;
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

    private final NotificationManager notificationManager = NotificationManager.getInstance();
    private final RewarmScheduler rewarmScheduler;

    public CacheManager(CacheConf cacheConf, RewarmScheduler rewarmScheduler) {
        if (cacheConf == null || !cacheConf.isEnabled()) {
            enabled = false;
            cache = null;
            this.rewarmScheduler = null;
            return;
        }
        enabled = true;
        this.rewarmScheduler = rewarmScheduler;
        this.cache = Collections.synchronizedMap(new LruDnsCache(cacheConf.getMaxCacheEntries()));
    }

    public DnsCacheEntry safeGet(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        return null;
    }

    public Message get(String key) {
        if (!enabled) {
            return null;
        }
        DnsCacheEntry entry = safeGet(key);
        if (entry != null && !entry.isExpired()) {
            log.info("cache hit for {}", key);
            entry.setRewarmCount(0);
            cache.put(key, entry);
            notificationManager.notify(EventType.CACHE_HIT);
            return entry.getResponse();
        }

        if (entry != null && entry.isExpired()) {
            log.info("cache entry {} was found, but expired. Removing", key);
            cache.remove(key);
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
        rewarmScheduler.schedule(key, entry.getExpiryTime());
    }

}