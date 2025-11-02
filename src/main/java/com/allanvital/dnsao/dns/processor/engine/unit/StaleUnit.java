package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StaleUnit extends AbstractCacheUnit {

    private static final int STALE_TTL = 30;

    public StaleUnit(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Message getFromCache(String key) {
        if (cacheManager == null) {
            return null;
        }
        DnsCacheEntry stale = cacheManager.getStale(key);
        if (stale == null) {
            return null;
        }
        return cloneWithNewTtl(stale.getResponse(), STALE_TTL);
    }

}
