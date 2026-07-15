package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import java.net.InetAddress;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveInternalRequestFactory {

    public DnsQueryRequest buildInternalQueryRequest(int type, String qname) {
        try {
            Message message = Message.newQuery(Record.newRecord(Name.fromString(normalize(qname)), type, DClass.IN));
            DnsQueryRequest request = new DnsQueryRequest(InetAddress.getLoopbackAddress());
            request.setOriginalRequest(message);
            request.setRequest(message);
            request.setIsLocalQuery(true);
            return request;
        } catch (Exception e) {
            throw new IllegalStateException("failed to create internal recursive request for qname=" + qname, e);
        }
    }

    private String normalize(String qname) {
        if (qname.endsWith(".")) {
            return qname;
        }
        return qname + ".";
    }
}
