package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.EngineUnit;
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

public class RecursiveSession {

    private static final String STUB_DOMAIN = "allanvital.com.";
    private static final String STUB_IP = "192.0.2.1";

    private final Message request;
    private final int timeoutMs;

    public RecursiveSession(Message request, int timeoutMs) {
        this.request = request;
        this.timeoutMs = timeoutMs;
    }

    public Message resolve() {
        Record question = request.getQuestion();

        if (question == null || question.getType() != Type.A) {
            return null;
        }

        Name qname = question.getName();
        if (!qname.equals(Name.fromConstantString(STUB_DOMAIN))) {
            return null;
        }

        return buildResponse(request, qname, question.getDClass());
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
            ARecord arec = new ARecord(qname, dclass, EngineUnit.DEFAULT_LOCAL_TTL, InetAddress.getByName(STUB_IP));
            response.addRecord(arec, Section.ANSWER);
        } catch (UnknownHostException e) {
            throw new RuntimeException("failed to create stub A record", e);
        }

        return response;
    }

    public QueryResolvedBy resolvedBy() {
        return QueryResolvedBy.RECURSION;
    }

}
