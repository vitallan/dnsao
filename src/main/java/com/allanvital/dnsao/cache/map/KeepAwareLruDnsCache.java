package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Record;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;

public class KeepAwareLruDnsCache extends LinkedHashMap<String, DnsCacheEntry> {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final int maxSize;
    private final KeepProvider keepProvider;
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
                return true;
            }
        }
        return false;
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
