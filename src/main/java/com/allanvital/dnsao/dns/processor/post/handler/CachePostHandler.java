package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit.key;
import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CachePostHandler implements PostHandler {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    private final CacheManager cacheManager;

    public CachePostHandler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        if (UPSTREAM.equals(response.getQueryResolvedBy()) && !request.isLocalQuery()) {
            log.debug("adding {} to cache", key(request.getRequest()));
            putInCache(request.getRequest(), response.getResponse());
        }
    }

    private void putInCache(Message request, Message response) {
        if (cacheManager == null) {
            return;
        }
        int rcode = response.getRcode();
        Long ttlFromDirectResponse = getTtlFromDirectResponse(response);
        if (ttlFromDirectResponse != null) {
            cacheManager.put(key(request), response, ttlFromDirectResponse);
            return;
        }
        if (rcode == Rcode.NXDOMAIN) {
            SOARecord soa = findSOA(response);
            long negTtl = (soa != null) ? negativeTtlFrom(soa) : 300;
            cacheManager.put(key(request), response, negTtl);
        } else if (rcode == Rcode.NOERROR && response.getSection(Section.ANSWER).isEmpty()) {
            SOARecord soa = findSOA(response);
            if (soa != null) {
                long negTtl = negativeTtlFrom(soa);
                cacheManager.put(key(request), response, negTtl);
            }
        }
    }

    public static SOARecord findSOA(Message msg) {
        for (Record r : msg.getSection(Section.AUTHORITY)) {
            if (r.getType() == Type.SOA) {
                return (SOARecord) r;
            }
        }
        return null;
    }

    public static long negativeTtlFrom(SOARecord soa) {
        long ttl = Math.min(soa.getMinimum(), soa.getTTL());
        ttl = Math.max(ttl, 60);
        ttl = Math.min(ttl, 300);
        return ttl;
    }

}
