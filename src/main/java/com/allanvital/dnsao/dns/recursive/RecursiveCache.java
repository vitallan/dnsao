package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheMessageSupport;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit;
import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveCache {

    private final CacheManager cacheManager;

    public RecursiveCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public StepResponse get(StepRequest request) {
        if (cacheManager == null || request == null) {
            return null;
        }

        DnsCacheEntry cacheEntry = cacheManager.get(AbstractCacheUnit.key(request.qname(), request.qtype()));
        if (cacheEntry == null) {
            return null;
        }

        long expiryTime = cacheEntry.getExpiryTime();
        long currentTime = Clock.currentTimeInMillis();
        long timeToExpireInMs = expiryTime - currentTime;
        long ttlInSecs = timeToExpireInMs <= 0 ? 1 : Math.max(1, Math.floorDiv(timeToExpireInMs, 1000));
        Message cachedResponse = CacheMessageSupport.cloneWithNewTtl(cacheEntry.getResponse(), ttlInSecs);
        return new StepResponse(cachedResponse);
    }

    public void put(StepRequest request, StepResponse response) {
        if (cacheManager == null || request == null || response == null) {
            return;
        }

        Message wireMessage = response.toWireMessage();
        Long ttl = CacheMessageSupport.resolveCacheTtl(wireMessage);
        if (ttl == null) {
            return;
        }

        cacheManager.put(AbstractCacheUnit.key(request.qname(), request.qtype()), wireMessage, ttl);
    }
}
