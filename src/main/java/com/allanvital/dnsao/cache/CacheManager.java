package com.allanvital.dnsao.cache;
import com.allanvital.dnsao.Constants;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.map.KeepAwareLruDnsCache;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

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
    private final KeepProvider keepProvider;
    private final AlwaysRewarmTopEntriesTracker alwaysRewarmTopEntriesTracker;

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
            this.keepProvider = null;
            this.alwaysRewarmTopEntriesTracker = null;
            return;
        }
        this.expiredConf = expiredConf;
        enabled = true;
        this.fixedTimeRewarmScheduler = fixedTimeRewarmScheduler;
        this.keepProvider = keepProvider;
        this.alwaysRewarmTopEntriesTracker = new AlwaysRewarmTopEntriesTracker(cacheConf.getAlwaysRewarmTopEntries());

        int maxEntries = cacheConf.getMaxCacheEntries();
        KeepAwareLruDnsCache keepAwareLruDnsCache = new KeepAwareLruDnsCache(maxEntries,
                keepProvider,
                Constants.STATS_BUCKET_INTERVAL_MS,
                Constants.STATS_WINDOW_MS,
                Clock::currentTimeInMillis,
                this::onCacheEntryRemoved);
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
            if (shouldRemove(key, entry)) {
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
            entry.setTransientRewarmFailureCount(0);
            cache.put(key, entry);
            recordClientAccess(key, entry);
            telemetryNotify(EventType.CACHE_HIT);
            return entry;
        }

        if (entry != null && entry.isStale()) {
            Log.CACHE.info("cache entry {} was found, but stale ", key);
            if (shouldRemove(key, entry)) {
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
        DnsCacheEntry currentEntry = cache.get(key);
        if (currentEntry != null) {
            entry.setLastAccessSeq(currentEntry.getLastAccessSeq());
        }
        addEntry(key, entry, false);
    }

    public boolean shouldAlwaysRewarm(String key, Record question) {
        if (!enabled || question == null) {
            return false;
        }
        if (isKeep(question)) {
            return true;
        }
        if (alwaysRewarmTopEntriesTracker == null) {
            return false;
        }
        return alwaysRewarmTopEntriesTracker.isProtected(key);
    }

    public void put(String key, Message response, Long ttlSecs) {
        if (!enabled) {
            return;
        }
        Log.CACHE.info("adding {} to cache", key);
        addEntry(key, new DnsCacheEntry(response, ttlSecs), true);
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
            if (entry != null && shouldRemove(key, entry)) {
                remove(key);
            }
        }
    }

    private void remove(String key) {
        Log.CACHE.info("removing {} from cache", key);
        cache.remove(key);
        onCacheEntryRemoved(key);
        telemetryNotify(CACHE_REMOVED);
    }

    private boolean shouldRemove(String key, DnsCacheEntry entry) {
        Record question = entry != null && entry.getResponse() != null ? entry.getResponse().getQuestion() : null;
        if (shouldAlwaysRewarm(key, question)) {
            return false;
        }
        if (!entry.isStale()) {
            return false;
        }
        if (expiredConf.isServeExpired() && !entry.isExpired(expiredConf.getServeExpiredMax())) {
            return false;
        }
        return true;
    }

    private void addEntry(String key, DnsCacheEntry entry, boolean recordAccess) {
        cache.put(key, entry);
        if (recordAccess) {
            recordClientAccess(key, entry);
        }
        telemetryNotify(CACHE_ADDED);
        fixedTimeRewarmScheduler.schedule(key, entry.getExpiryTime());
    }

    private void recordClientAccess(String key, DnsCacheEntry entry) {
        if (alwaysRewarmTopEntriesTracker == null || entry == null) {
            return;
        }
        Record question = entry.getResponse() != null ? entry.getResponse().getQuestion() : null;
        if (isKeep(question)) {
            return;
        }
        long accessSeq = alwaysRewarmTopEntriesTracker.recordAccess(key);
        entry.setLastAccessSeq(accessSeq);
    }

    private void onCacheEntryRemoved(String key) {
        if (fixedTimeRewarmScheduler != null) {
            fixedTimeRewarmScheduler.cancel(key);
        }
        if (alwaysRewarmTopEntriesTracker != null) {
            alwaysRewarmTopEntriesTracker.remove(key);
        }
    }

    private boolean isKeep(Record question) {
        return question != null && keepProvider != null && keepProvider.contain(question);
    }

}
