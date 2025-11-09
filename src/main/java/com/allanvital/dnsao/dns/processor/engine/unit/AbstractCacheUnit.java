package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
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
    public DnsQueryResponse process(DnsQueryRequest dnsQueryRequest) {
        if (dnsQueryRequest.isLocalQuery()) {
            return null; //queries coming from dnsao itself should not hit cache to allow for rewarm
        }
        Message query = dnsQueryRequest.getRequest();
        Message cached = getFromCache(key(query));
        if (cached != null) {
            Header header = cached.getHeader();
            header.setID(query.getHeader().getID());
            cached.setHeader(header);
            return new DnsQueryResponse(dnsQueryRequest, cached, CACHE);
        }
        return null;
    }

    protected Message cloneWithNewTtl(Message message, long newTtl) {
        Message copy = new Message();
        Header origHeader = message.getHeader();
        Header newHeader = copy.getHeader();

        newHeader.setID(origHeader.getID());
        newHeader.setOpcode(origHeader.getOpcode());
        newHeader.setRcode(origHeader.getRcode());

        int[] COPY_FLAGS = { Flags.QR, Flags.AA, Flags.TC, Flags.RD, Flags.RA, Flags.AD, Flags.CD };

        for (int flag : COPY_FLAGS) {
            if (origHeader.getFlag(flag)){
                newHeader.setFlag(flag);
            }
        }

        for (int section = 0; section < 4; section++) {
            List<Record> records = message.getSection(section);
            if (records == null || records.isEmpty()) {
                continue;
            }
            for (Record record : records) {
                if (record == null) {
                    continue;
                }
                int type = record.getType();
                if (section == Section.QUESTION || type == Type.OPT || type == Type.TSIG) {
                    copy.addRecord(record, section);
                    continue;
                }
                byte[] rdata = record.rdataToWireCanonical();
                Record newRecord = Record.newRecord(
                        record.getName(),
                        type,
                        record.getDClass(),
                        newTtl,
                        rdata
                );

                copy.addRecord(newRecord, section);
            }
        }
        return copy;
    }

}
