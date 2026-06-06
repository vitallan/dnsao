package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.Constants;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Record;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;

public class KeepAwareLruDnsCache extends LinkedHashMap<String, DnsCacheEntry> implements CacheStats {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final int maxSize;
    private final KeepProvider keepProvider;
    private final ConcurrentLinkedDeque<Long> evictionTimes = new ConcurrentLinkedDeque<>();
    private boolean warnedAllKeepOverCapacity;

    public KeepAwareLruDnsCache(int maxSize, KeepProvider keepProvider) {
        super(Math.max(16, maxSize), 0.75f, true);
        this.maxSize = maxSize;
        this.keepProvider = keepProvider;
    }

    @Override
    public DnsCacheEntry put(String key, DnsCacheEntry value) {
        DnsCacheEntry prev = super.put(key, value);
        trimIfNeeded();
        return prev;
    }

    private void trimIfNeeded() {
        if (maxSize <= 0) {
            return;
        }
        while (size() > maxSize) {
            boolean removed = removeEldestNonKeep();
            if (removed) {
                continue;
            }

            if (!warnedAllKeepOverCapacity) {
                warnedAllKeepOverCapacity = true;
                log.warn("cache over capacity (size={}, max={}) but all entries are in keep; allowing size to exceed maxCacheEntries", size(), maxSize);
            }
            break;
        }
    }

    private boolean removeEldestNonKeep() {
        Iterator<Map.Entry<String, DnsCacheEntry>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DnsCacheEntry> e = it.next();
            if (e == null) {
                continue;
            }
            DnsCacheEntry entry = e.getValue();
            if (!isKeep(entry)) {
                it.remove();
                evictionTimes.addLast(Clock.currentTimeInMillis());
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCurrentSize() {
        return size();
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public long getEvictionCount() {
        long now = Clock.currentTimeInMillis();
        long cutoff = now - Constants.STATS_WINDOW_MS;
        while (!evictionTimes.isEmpty() && evictionTimes.peekFirst() < cutoff) {
            evictionTimes.pollFirst();
        }
        return evictionTimes.size();
    }

    private boolean isKeep(DnsCacheEntry entry) {
        if (keepProvider == null || entry == null) {
            return false;
        }
        try {
            Record q = entry.getResponse() != null ? entry.getResponse().getQuestion() : null;
            return q != null && keepProvider.contain(q);
        } catch (Throwable t) {
            return false;
        }
    }
}
