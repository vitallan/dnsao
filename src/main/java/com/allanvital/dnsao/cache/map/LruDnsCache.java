package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.allanvital.dnsao.AppLoggers.CACHE;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LruDnsCache extends LinkedHashMap<String, DnsCacheEntry> {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final int maxSize;

    public LruDnsCache(int maxSize) {
        super(maxSize, 0.75f, true); // accessOrder = true
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, DnsCacheEntry> eldest) {
        return size() > maxSize;
    }

}