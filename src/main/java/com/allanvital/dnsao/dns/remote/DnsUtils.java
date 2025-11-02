package com.allanvital.dnsao.dns.remote;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsUtils {

    public static Message formErr(Message req) {
        Message m = new Message(req.getHeader().getID());
        m.getHeader().setFlag(Flags.QR);
        m.getHeader().setRcode(Rcode.FORMERR);
        return m;
    }

    public static Message baseResponse(Message request, Record question) {
        Message resp = new Message(request.getHeader().getID());

        Header h = resp.getHeader();
        h.setFlag(Flags.QR);
        if (request.getHeader().getFlag(Flags.RD)) h.setFlag(Flags.RD);
        h.setFlag(Flags.RA);

        resp.addRecord(question, Section.QUESTION);

        OPTRecord opt = request.getOPT();
        if (opt != null) resp.addRecord(opt, Section.ADDITIONAL);

        return resp;
    }

    public static boolean isDirectAnswer(int type) {
        return switch (type) {
            case Type.A, Type.AAAA, Type.CNAME, Type.HTTPS, Type.SVCB -> true;
            default -> false;
        };
    }

    public static boolean isWarmable(Message msg) {
        return getTtlFromDirectResponse(msg) != null;
    }

    public static Long getTtlFromDirectResponse(Message message) {
        if (message == null || message.getRcode() != Rcode.NOERROR) {
            return null;
        }
        List<Record> section = message.getSection(Section.ANSWER);
        if (section == null || section.isEmpty()) {
            return null;
        }
        for (Record r : section) {
            if (isDirectAnswer(r.getType())) {
                return r.getTTL();
            }
        }
        return null;
    }

}