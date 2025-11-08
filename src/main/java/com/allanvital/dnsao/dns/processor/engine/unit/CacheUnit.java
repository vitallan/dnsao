package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;
import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheUnit extends AbstractCacheUnit {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

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
            long expiryTime = dnsCacheEntry.getExpiryTime();
            long currentTime = Clock.currentTimeInMillis();
            long timeToExpireInMs = expiryTime - currentTime;
            long ttlInSecs = timeToExpireInMs <= 0 ? 1 : Math.max(1, Math.floorDiv(timeToExpireInMs, 1000));
            log.trace("TTL calculated in cache in millis {} - {} = {}",
                    formatMillis(expiryTime, "HH:mm:ss.SSS"),
                    formatMillis(currentTime, "HH:mm:ss.SSS"),
                    timeToExpireInMs);
            return cloneWithNewTtl(dnsCacheEntry.getResponse(), ttlInSecs);
        }
        return null;
    }

}
