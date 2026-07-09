package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.List;

import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

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
        if (dnsCacheEntry == null) {
            return null;
        }

        Message cached = dnsCacheEntry.getResponse();
        List<org.xbill.DNS.Record> answerSection = cached.getSection(Section.ANSWER);
        if (answerSection.isEmpty()) {
            boolean hasNsReferral = cached.getSection(Section.AUTHORITY).stream()
                    .anyMatch(r -> r.getType() == Type.NS);
            if (hasNsReferral) {
                Log.CACHE.trace("skipping cached delegation for key={} (no ANSWER, NS in AUTHORITY)", key);
                return null;
            }
        }

        long expiryTime = dnsCacheEntry.getExpiryTime();
        long currentTime = Clock.currentTimeInMillis();
        long timeToExpireInMs = expiryTime - currentTime;
        long ttlInSecs = timeToExpireInMs <= 0 ? 1 : Math.max(1, Math.floorDiv(timeToExpireInMs, 1000));
        Log.CACHE.trace("TTL calculated in cache in millis {} - {} = {}",
                formatMillis(expiryTime, "HH:mm:ss.SSS"),
                formatMillis(currentTime, "HH:mm:ss.SSS"),
                timeToExpireInMs);
        return cloneWithNewTtl(cached, ttlInSecs);
    }

}
