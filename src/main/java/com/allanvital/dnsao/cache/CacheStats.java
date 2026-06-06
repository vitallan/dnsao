package com.allanvital.dnsao.cache;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface CacheStats {

    int getCurrentSize();

    int getMaxSize();

    long getEvictionCount();

}
