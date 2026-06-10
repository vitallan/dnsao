package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import static org.xbill.DNS.Opcode.QUERY;

public class StepRequest {

    private final Name qname;
    private final int qtype;
    private final int qclass;

    public StepRequest(Name qname, int qtype, int qclass) {
        this.qname = qname;
        this.qtype = qtype;
        this.qclass = qclass;
    }

    public static StepRequest fromMessage(DnsQueryRequest dnsQueryRequest) {
        if (dnsQueryRequest == null || dnsQueryRequest.getRequest() == null) {
            return null;
        }
        Message request = dnsQueryRequest.getRequest();
        Record question = request.getQuestion();
        if (question == null) {
            return null;
        }
        return new StepRequest(question.getName(), question.getType(), question.getDClass());
    }

    public Name qname() {
        return qname;
    }

    public int qtype() {
        return qtype;
    }

    public int qclass() {
        return qclass;
    }

    public Message toWireMessage() {
        Record question = Record.newRecord(qname, qtype, qclass);
        Message message = new Message();
        message.getHeader().setOpcode(QUERY);
        message.addRecord(question, Section.QUESTION);
        return message;
    }

}
