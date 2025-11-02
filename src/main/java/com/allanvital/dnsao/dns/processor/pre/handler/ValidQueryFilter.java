package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ValidQueryFilter implements PreHandler {

    private static final Set<Integer> ALLOWED_TYPES = Set.of(
            Type.A,
            Type.AAAA,
            Type.CNAME,
            Type.MX,
            Type.NS,
            Type.SOA,
            Type.PTR,
            Type.TXT,
            Type.SRV,
            Type.CAA,
            Type.HTTPS
    );

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        Record question = message.getQuestion();
        Name name = question.getName();
        int type = question.getType();
        String typeName = Type.string(type);
        if (!ALLOWED_TYPES.contains(type)) {
            Message refused = buildRefused(question, message.getHeader().getID());
            throw new PreHandlerException(refused, "Non-allowed query type " + typeName + " to " + name);
        }
        return message;
    }

    private Message buildRefused(org.xbill.DNS.Record question, int queryId) {
        Message refused = new Message(queryId);
        refused.getHeader().setFlag(Flags.QR);
        refused.getHeader().setRcode(Rcode.REFUSED);
        refused.addRecord(question, Section.QUESTION);
        return refused;
    }

}
