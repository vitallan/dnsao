package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecQueryShaper {

    public static Message prepareUpstreamQuery(Message query, DNSSecMode mode) {
        removeOPT(query);

        Header h = query.getHeader();
        h.unsetFlag(Flags.AD);
        h.unsetFlag(Flags.CD);
        h.setFlag(Flags.RD);

        final int udpPayload = 1232;
        final int xrcode = 0;
        final int version = 0;

        switch (mode) {
            case OFF -> {
                OPTRecord opt = new OPTRecord(udpPayload, xrcode, version, 0);
                query.addRecord(opt, Section.ADDITIONAL);
            }
            case SIMPLE, RIGID -> {
                OPTRecord opt = new OPTRecord(udpPayload, xrcode, version, ExtendedFlags.DO);
                query.addRecord(opt, Section.ADDITIONAL);
            }
        }
        return query;
    }

    private static void removeOPT(Message message) {
        List<Record> section = message.getSection(Section.ADDITIONAL);
        message.removeAllRecords(Section.ADDITIONAL);
        for (Record record : section) {
            if (record instanceof OPTRecord) {
                continue;
            }
            message.addRecord(record, Section.ADDITIONAL);
        }
    }

}
