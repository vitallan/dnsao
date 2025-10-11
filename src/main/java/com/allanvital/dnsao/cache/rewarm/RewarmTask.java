package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmTask {

    private final String key;
    private final long triggerAtMs;
    private final DnsCacheEntry dnsCacheEntry;

    public RewarmTask(String key, long triggerAtMs, DnsCacheEntry dnsCacheEntry) {
        this.key = key;
        this.triggerAtMs = triggerAtMs;
        this.dnsCacheEntry = dnsCacheEntry;
    }

    public String getKey() {
        return key;
    }

    public long getTriggerAtMs() {
        return triggerAtMs;
    }

    public DnsCacheEntry getDnsCacheEntry() {
        return dnsCacheEntry;
    }
}