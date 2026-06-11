package com.allanvital.dnsao.cache;

import com.allanvital.dnsao.dns.remote.DnsUtils;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface CacheMessageSupport {

    static Long resolveCacheTtl(Message response) {
        if (response == null) {
            return null;
        }

        Long ttlFromDirectResponse = DnsUtils.getTtlFromDirectResponse(response);
        if (ttlFromDirectResponse != null) {
            return ttlFromDirectResponse;
        }

        int rcode = response.getRcode();
        if (rcode == Rcode.NXDOMAIN) {
            SOARecord soa = findSOA(response);
            return soa != null ? negativeTtlFrom(soa) : 300L;
        }

        if (rcode != Rcode.NOERROR) {
            return null;
        }

        List<Record> answers = response.getSection(Section.ANSWER);
        if (answers != null && !answers.isEmpty()) {
            return null;
        }

        SOARecord soa = findSOA(response);
        if (soa != null) {
            return negativeTtlFrom(soa);
        }

        return delegationTtlFrom(response);
    }

    static Message cloneWithNewTtl(Message message, long newTtl) {
        Message copy = new Message();
        Header origHeader = message.getHeader();
        Header newHeader = copy.getHeader();

        newHeader.setID(origHeader.getID());
        newHeader.setOpcode(origHeader.getOpcode());
        newHeader.setRcode(origHeader.getRcode());

        int[] copyFlags = {Flags.QR, Flags.AA, Flags.TC, Flags.RD, Flags.RA, Flags.AD, Flags.CD};
        for (int flag : copyFlags) {
            if (origHeader.getFlag(flag)) {
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
                Record newRecord = Record.newRecord(record.getName(), type, record.getDClass(), newTtl, rdata);
                copy.addRecord(newRecord, section);
            }
        }
        return copy;
    }

    static SOARecord findSOA(Message message) {
        for (Record record : message.getSection(Section.AUTHORITY)) {
            if (record.getType() == Type.SOA) {
                return (SOARecord) record;
            }
        }
        return null;
    }

    static long negativeTtlFrom(SOARecord soa) {
        long ttl = Math.min(soa.getMinimum(), soa.getTTL());
        ttl = Math.max(ttl, 60);
        ttl = Math.min(ttl, 300);
        return ttl;
    }

    static Long delegationTtlFrom(Message message) {
        long minTtl = Long.MAX_VALUE;
        boolean foundNs = false;
        for (Record record : message.getSection(Section.AUTHORITY)) {
            if (record instanceof NSRecord) {
                foundNs = true;
                minTtl = Math.min(minTtl, record.getTTL());
            }
        }
        if (!foundNs) {
            return null;
        }
        return minTtl;
    }
}
