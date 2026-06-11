package com.allanvital.dnsao.dns.processor.post.handler;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheMessageSupport;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import static com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit.key;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.RECURSION;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CachePostHandler implements PostHandler {


    private final CacheManager cacheManager;

    public CachePostHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        if ((UPSTREAM.equals(response.getQueryResolvedBy()) || RECURSION.equals(response.getQueryResolvedBy())) && !request.isLocalQuery()) {
            Log.DNS.debug("adding {} to cache", key(request.getRequest()));
            putInCache(request.getRequest(), response.getResponse());
        }
    }

    private void putInCache(Message request, Message response) {
        if (cacheManager == null) {
            return;
        }
        Long ttl = CacheMessageSupport.resolveCacheTtl(response);
        if (ttl != null) {
            cacheManager.put(key(request), response, ttl);
        }
    }

    public static SOARecord findSOA(Message msg) {
        return CacheMessageSupport.findSOA(msg);
    }

    public static long negativeTtlFrom(SOARecord soa) {
        return CacheMessageSupport.negativeTtlFrom(soa);
    }

}
