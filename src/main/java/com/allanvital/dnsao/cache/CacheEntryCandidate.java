package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheEntryCandidate {

    private final boolean cacheable;
    private final DnsCacheEntry dnsCacheEntry;

    private CacheEntryCandidate(boolean cacheable, DnsCacheEntry dnsCacheEntry) {
        this.cacheable = cacheable;
        this.dnsCacheEntry = dnsCacheEntry;
    }

    public static CacheEntryCandidate cacheable(DnsCacheEntry dnsCacheEntry) {
        return new CacheEntryCandidate(true, dnsCacheEntry);
    }

    public static CacheEntryCandidate notCacheable() {
        return new CacheEntryCandidate(false, null);
    }

    public boolean isCacheable() {
        return cacheable && this.getDnsCacheEntry() != null;
    }

    public DnsCacheEntry getDnsCacheEntry() {
        return dnsCacheEntry;
    }
}
