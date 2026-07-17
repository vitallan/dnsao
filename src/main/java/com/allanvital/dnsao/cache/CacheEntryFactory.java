package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheEntryFactory {

    public CacheEntryCandidate build(Message response) {
        if (response == null) {
            return CacheEntryCandidate.notCacheable();
        }
        Long ttlFromDirectResponse = getTtlFromDirectResponse(response);
        if (ttlFromDirectResponse != null) {
            return CacheEntryCandidate.cacheable(new DnsCacheEntry(response, ttlFromDirectResponse));
        }
        int rcode = response.getRcode();
        SOARecord soa = findSOA(response);
        if (rcode == Rcode.NXDOMAIN && soa != null) {
            return CacheEntryCandidate.cacheable(new DnsCacheEntry(response, negativeTtlFrom(soa)));
        }
        if (rcode == Rcode.NOERROR && response.getSection(Section.ANSWER).isEmpty() && soa != null) {
            return CacheEntryCandidate.cacheable(new DnsCacheEntry(response, negativeTtlFrom(soa)));
        }
        return CacheEntryCandidate.notCacheable();
    }

    private SOARecord findSOA(Message msg) {
        for (Record record : msg.getSection(Section.AUTHORITY)) {
            if (record.getType() == Type.SOA) {
                return (SOARecord) record;
            }
        }
        return null;
    }

    private long negativeTtlFrom(SOARecord soa) {
        long ttl = Math.min(soa.getMinimum(), soa.getTTL());
        ttl = Math.max(ttl, 60);
        ttl = Math.min(ttl, 300);
        return ttl;
    }
}
