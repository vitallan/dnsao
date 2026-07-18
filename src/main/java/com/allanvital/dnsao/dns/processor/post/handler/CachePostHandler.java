package com.allanvital.dnsao.dns.processor.post.handler;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.CacheEntryFactory;
import com.allanvital.dnsao.cache.CacheEntryCandidate;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.remote.UpstreamRoutingPolicy;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit.key;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CachePostHandler implements PostHandler {


    private final CacheManager cacheManager;
    private final CacheEntryFactory cacheEntryFactory;

    public CachePostHandler(CacheManager cacheManager, CacheEntryFactory cacheEntryFactory) {
        this.cacheManager = cacheManager;
        this.cacheEntryFactory = cacheEntryFactory;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        if (UPSTREAM.equals(response.getQueryResolvedBy()) && !request.isLocalQuery()) {
            Log.DNS.debug("adding {} to cache", key(request.getRequest()));
            putInCache(request.getRequest(), response.getResponse(), request.getUpstreamRoutingPolicy());
        }
    }

    private void putInCache(Message request, Message response, UpstreamRoutingPolicy upstreamRoutingPolicy) {
        if (cacheManager == null) {
            return;
        }
        CacheEntryCandidate result = cacheEntryFactory.build(response);
        if (result.isCacheable()) {
            cacheManager.put(key(request), response, result.getDnsCacheEntry().getConfiguredTtlInSeconds(), upstreamRoutingPolicy);
        }
    }

}
