package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheMessageSupport;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;

import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.CACHE;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractCacheUnit implements EngineUnit {

    protected final CacheManager cacheManager;

    public AbstractCacheUnit(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    protected abstract Message getFromCache(String key);

    public static String key(Message message) {
        return key(message.getQuestion());
    }

    public static String key(Record record) {
        Name name = record.getName();
        int type = record.getType();
        return key(name, type);
    }

    public static String key(Name questionName, int type) {
        String typeName = Type.string(type);
        return typeName + ":" + questionName;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        if (dnsQueryRequest.isLocalQuery()) {
            return null; //queries coming from dnsao itself should not hit cache to allow for rewarm
        }
        Message query = dnsQueryRequest.getRequest();
        Message cached = getFromCache(key(query));
        if (cached != null) {
            Header header = cached.getHeader();
            header.setID(query.getHeader().getID());
            cached.setHeader(header);
            return new DnsQueryResponse(dnsQueryRequest, cached);
        }
        return null;
    }

    protected Message cloneWithNewTtl(Message message, long newTtl) {
        return CacheMessageSupport.cloneWithNewTtl(message, newTtl);
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return CACHE;
    }

}
