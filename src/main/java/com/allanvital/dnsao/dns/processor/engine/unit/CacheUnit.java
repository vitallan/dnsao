package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheUnit extends AbstractCacheUnit {

    public CacheUnit(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Message getFromCache(String key) {
        if (cacheManager == null) {
            return null;
        }
        DnsCacheEntry dnsCacheEntry = cacheManager.get(key);
        if (dnsCacheEntry != null) {
            long timeToExpireInMs = dnsCacheEntry.getExpiryTime() - Clock.currentTimeInMillis();
            return cloneWithNewTtl(dnsCacheEntry.getResponse(), timeToExpireInMs / 1000);
        }
        return null;
    }

}
