package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheMessageSupport;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveCache {

    private final CacheManager cacheManager;
    private final RecursiveStatsCollector recursiveStatsCollector;

    public RecursiveCache(CacheManager cacheManager) {
        this(cacheManager, new NoOpRecursiveStatsCollector());
    }

    public RecursiveCache(CacheManager cacheManager, RecursiveStatsCollector recursiveStatsCollector) {
        this.cacheManager = cacheManager;
        this.recursiveStatsCollector = recursiveStatsCollector;
    }

    public StepResponse get(StepRequest request) {
        if (cacheManager == null || request == null) {
            return null;
        }

        DnsCacheEntry cacheEntry = cacheManager.get(AbstractCacheUnit.key(request.qname(), request.qtype()));
        if (cacheEntry == null) {
            return null;
        }

        recursiveStatsCollector.increment(cacheMetricFor(request, true));
        Log.CACHE.trace("recursive cache hit type={} qtype={} qname={}", cacheTypeFor(request), Type.string(request.qtype()), request.qname());

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

        recursiveStatsCollector.increment(cacheMetricFor(request, false));
        Log.CACHE.trace("recursive cache put type={} qtype={} qname={} ttl={}", cacheTypeFor(request), Type.string(request.qtype()), request.qname(), ttl);
        cacheManager.put(AbstractCacheUnit.key(request.qname(), request.qtype()), wireMessage, ttl);
    }

    private RecursiveMetric cacheMetricFor(StepRequest request, boolean hit) {
        if (request.qtype() == Type.NS) {
            return hit ? RecursiveMetric.CACHE_HIT_NS : RecursiveMetric.CACHE_PUT_NS;
        }
        return hit ? RecursiveMetric.CACHE_HIT_FINAL : RecursiveMetric.CACHE_PUT_FINAL;
    }

    private String cacheTypeFor(StepRequest request) {
        return request.qtype() == Type.NS ? "ns" : "final";
    }
}
