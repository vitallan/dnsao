package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.OFF;
import static com.allanvital.dnsao.dns.processor.pre.handler.opt.Constants.DEFAULT_UDP_PAYLOAD;
import static org.xbill.DNS.Opcode.QUERY;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StepRequest {

    private final Name qname;
    private final int qtype;
    private final int qclass;
    private final boolean dnssecEnabled;

    public StepRequest(Name qname, int qtype, int qclass, DNSSecMode dnsSecMode) {
        this.qname = qname;
        this.qtype = qtype;
        this.qclass = qclass;
        this.dnssecEnabled = !OFF.equals(dnsSecMode);
    }

    public static StepRequest fromMessage(DnsQueryRequest dnsQueryRequest, DNSSecMode dnsSecMode) {
        if (dnsQueryRequest == null || dnsQueryRequest.getRequest() == null) {
            return null;
        }
        Message request = dnsQueryRequest.getRequest();
        Record question = request.getQuestion();
        if (question == null) {
            return null;
        }
        return new StepRequest(question.getName(), question.getType(), question.getDClass(), dnsSecMode);
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

    public boolean dnssecEnabled() {
        return dnssecEnabled;
    }

    public Message toWireMessage() {
        Record question = Record.newRecord(qname, qtype, qclass);
        Message message = new Message();
        message.getHeader().setOpcode(QUERY);
        message.addRecord(question, Section.QUESTION);
        message.addRecord(new OPTRecord(DEFAULT_UDP_PAYLOAD, 0, 0, dnssecEnabled ? ExtendedFlags.DO : 0), Section.ADDITIONAL);
        return message;
    }

}
