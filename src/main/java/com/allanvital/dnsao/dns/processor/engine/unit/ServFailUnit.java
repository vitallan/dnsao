package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServFailUnit implements EngineUnit {

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        Message servfail = buildServFail(dnsQueryRequest.getRequest());
        return new DnsQueryResponse(dnsQueryRequest, servfail);
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return SERVFAIL;
    }

    private Message buildServFail(Message query) {
        Message fail = new Message(query.getHeader().getID());
        fail.getHeader().setFlag(Flags.QR);
        fail.addRecord(query.getQuestion(), Section.QUESTION);
        fail.getHeader().setRcode(Rcode.SERVFAIL);
        return fail;
    }

}
