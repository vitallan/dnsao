package com.allanvital.dnsao.cache;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface CacheStats {

    int getCurrentSize();

    int getMaxSize();

    long getEvictionCount();

    default List<SizeSnapshot> getSizeHistory() {
        return List.of();
    }

}
