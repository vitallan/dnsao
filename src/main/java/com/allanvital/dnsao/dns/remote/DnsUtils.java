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

    public static Long getTtlFromDirectResponse(Message message) {
        if (message == null || message.getRcode() != Rcode.NOERROR) {
            return null;
        }
        List<Record> section = message.getSection(Section.ANSWER);
        if (section == null || section.isEmpty()) {
            return null;
        }
        List<Integer> directAnswerTypes = List.of(Type.A, Type.AAAA, Type.CNAME, Type.HTTPS, Type.SVCB);
        for (Record r : section) {
            if (directAnswerTypes.contains(r.getType())) {
                return r.getTTL();
            }
        }
        return null;
    }

    public static String extractIpFromResponseMessage(Message response) {
        String toReturn = "";
        ARecord aRecord = extractARecordFromMessage(response);
        AAAARecord aaaaRecord = extractAAAARecordFromMessage(response);
        if (aRecord != null) {
            toReturn = aRecord.getAddress().getHostAddress();
        } else if(aaaaRecord != null) {
            toReturn = aaaaRecord.getAddress().getHostAddress();
        } else {
            toReturn = "";
        }
        return toReturn;
    }

    private static ARecord extractARecordFromMessage(Message response) {
        return extractTypeFromMessage(response, ARecord.class);
    }

    private static AAAARecord extractAAAARecordFromMessage(Message response) {
        return extractTypeFromMessage(response, AAAARecord.class);
    }

    private static <T extends Record> T extractTypeFromMessage(Message response, Class<T> clazz) {
        List<Record> section = response.getSection(Section.ANSWER);
        for (Record record : section) {
            if (clazz.isInstance(record)) {
                return (T) record;
            }
        }
        return null;
    }

}