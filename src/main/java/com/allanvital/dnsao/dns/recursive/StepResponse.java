package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.ArrayList;
import java.util.List;

public class StepResponse {

    private final Message message;

    public StepResponse(Message message) {
        this.message = message;
    }

    public boolean isNXDOMAIN() {
        return message.getRcode() == Rcode.NXDOMAIN;
    }

    public boolean hasAnswer(Name qname, int qtype) {
        for (Record r : message.getSection(Section.ANSWER)) {
            if (r.getName().equals(qname) && r.getType() == qtype) {
                return true;
            }
        }
        return false;
    }

    public Name getCnameTarget(Name qname) {
        for (Record r : message.getSection(Section.ANSWER)) {
            if (r.getType() == Type.CNAME && r.getName().equals(qname)) {
                return ((org.xbill.DNS.CNAMERecord) r).getTarget();
            }
        }
        return null;
    }

    public List<NameServerAddress> getReferralServers() {
        List<NSRecord> nsRecords = new ArrayList<>();
        for (Record r : message.getSection(Section.AUTHORITY)) {
            if (r instanceof NSRecord ns) {
                nsRecords.add(ns);
            }
        }
        if (nsRecords.isEmpty()) {
            return List.of();
        }

        List<NameServerAddress> result = new ArrayList<>();
        for (NSRecord ns : nsRecords) {
            Name target = ns.getTarget();
            for (Record r : message.getSection(Section.ADDITIONAL)) {
                if (r instanceof ARecord a && r.getName().equals(target)) {
                    result.add(new NameServerAddress(a.getAddress().getHostAddress(), 53));
                }
            }
        }
        return result;
    }

    public Message toWireMessage() {
        return message;
    }

}
