package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RecursiveUnit implements EngineUnit {

    private static final String STUB_DOMAIN = "allanvital.com.";
    private static final String STUB_IP = "192.0.2.1";

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        Message request = dnsQueryRequest.getRequest();
        Record question = request.getQuestion();

        if (question == null || question.getType() != Type.A) {
            return null;
        }

        Name qname = question.getName();
        if (!qname.equals(Name.fromConstantString(STUB_DOMAIN))) {
            return null;
        }

        Message response = buildResponse(request, qname, question.getDClass());
        return new DnsQueryResponse(dnsQueryRequest, response);
    }

    private Message buildResponse(Message request, Name qname, int dclass) {
        Header qh = request.getHeader();
        Message response = new Message(qh.getID());
        Header rh = response.getHeader();

        rh.setOpcode(qh.getOpcode());
        rh.setFlag(Flags.QR);
        rh.setFlag(Flags.RA);

        if (qh.getFlag(Flags.RD)) {
            rh.setFlag(Flags.RD);
        }

        response.addRecord(request.getQuestion(), Section.QUESTION);

        try {
            ARecord arec = new ARecord(qname, dclass, DEFAULT_LOCAL_TTL, InetAddress.getByName(STUB_IP));
            response.addRecord(arec, Section.ANSWER);
        } catch (UnknownHostException e) {
            throw new RuntimeException("failed to create stub A record", e);
        }

        return response;
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return QueryResolvedBy.RECURSION;
    }

}
