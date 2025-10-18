package com.allanvital.dnsao.helper;

import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MessageUtils {

    public static Message buildARequest(String domain) {
        return buildARequest(domain, false);
    }

    public static Message buildARequest(String domain, boolean withDO) {
        String domainToUse = domain;
        try {
            if (!domain.endsWith(".")) {
                domainToUse = domain + ".";
            }
            Message message = Message.newQuery(Record.newRecord(Name.fromString(domainToUse), Type.A, DClass.IN));
            if (withDO) {
                OPTRecord opt = new OPTRecord(1232, 0, 0, ExtendedFlags.DO);
                message.addRecord(opt, Section.ADDITIONAL);
            }
            return message;
        } catch (TextParseException e) {
            Assertions.fail("failed to create message " + e.getMessage());
            return null;
        }
    }

    private static Message baseResponseFrom(Message request) {
        Header qh = request.getHeader();

        Message response = new Message(qh.getID());
        Header rh = response.getHeader();

        rh.setOpcode(qh.getOpcode());

        rh.setFlag(Flags.QR);

        if (qh.getFlag(Flags.RD)) {
            rh.setFlag(Flags.RD);
        }

        Record question = request.getQuestion();
        if (question != null) {
            response.addRecord(question, Section.QUESTION);
        }
        return response;
    }

    public static Message buildAResponse(Message request, String ipAddress, long ttl) {
        return buildAResponse(request, ipAddress, ttl, false);
    }

    public static Message buildAResponse(Message request, String ipAddress, long ttl, boolean authenticated) {
        try {
            Message response = baseResponseFrom(request);

            Record question = request.getQuestion();
            if (question != null && question.getType() == Type.A) {
                Name qname = question.getName();
                int qclass = question.getDClass();

                ARecord arec = new ARecord(qname, qclass, ttl, InetAddress.getByName(ipAddress));
                response.addRecord(arec, Section.ANSWER);
            }

            if (authenticated) {
                response.getHeader().setFlag(Flags.AD);
            } else {
                response.getHeader().unsetFlag(Flags.AD);
            }

            return response;
        } catch (IOException e) {
            Assertions.fail("failed to create A response: " + e.getMessage());
            return null;
        }
    }

    public static Message buildNxdomainResponseFrom(Message request, boolean authenticated) {
        Message response = baseResponseFrom(request);
        Header h = response.getHeader();
        h.setRcode(Rcode.NXDOMAIN);

        if (authenticated) {
            h.setFlag(Flags.AD);
        } else {
            h.unsetFlag(Flags.AD);
        }
        return response;
    }

    public static Message buildServfailFrom(Message request) {
        Message response = baseResponseFrom(request);
        Header h = response.getHeader();
        h.setRcode(Rcode.SERVFAIL);
        h.unsetFlag(Flags.AD);
        return response;
    }

    public static String extractIpFromResponseMessage(Message response) {
        List<Record> section = response.getSection(Section.ANSWER);
        Assertions.assertNotNull(section);
        Assertions.assertFalse(section.isEmpty());
        String ip = "";
        for (Record record : section) {
            if (record instanceof ARecord) {
                ARecord arec = (ARecord) record;
                ip = arec.getAddress().getHostAddress();
                return ip;
            }
        }
        Assertions.fail("no ip was found on message");
        return null;
    }

}