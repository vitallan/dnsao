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
        String domainToUse = domain;
        try {
            if (!domain.endsWith(".")) {
                domainToUse = domain + ".";
            }
            return Message.newQuery(Record.newRecord(Name.fromString(domainToUse), Type.A, DClass.IN));
        } catch (TextParseException e) {
            Assertions.fail("failed to create message " + e.getMessage());
            return null;
        }
    }

    public static Message buildAResponse(Message request, String ipAddress, long ttl) {
        try {
            Record question = request.getQuestion();
            Name qname = question.getName();
            int qtype = question.getType();
            int qclass = question.getDClass();

            Message response = new Message(request.getHeader().getID());
            Header header = response.getHeader();
            header.setFlag(Flags.QR);

            response.addRecord(question, Section.QUESTION);

            if (qtype == Type.A) {
                InetAddress address = InetAddress.getByName(ipAddress);
                ARecord aRecord = new ARecord(qname, qclass, ttl, address);
                response.addRecord(aRecord, Section.ANSWER);
            }

            return response;
        } catch (IOException e) {
            Assertions.fail("failed to create response " + e.getMessage());
            return null;
        }
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