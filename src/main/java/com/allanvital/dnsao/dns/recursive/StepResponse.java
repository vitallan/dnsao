package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
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
                return ((CNAMERecord) r).getTarget();
            }
        }
        return null;
    }

    public List<Record> getCnameAnswers(Name qname) {
        List<Record> result = new ArrayList<>();
        for (Record r : message.getSection(Section.ANSWER)) {
            if (r.getType() == Type.CNAME && r.getName().equals(qname)) {
                result.add(r);
            }
        }
        return result;
    }

    public List<Record> getAnswerRecords() {
        return List.copyOf(message.getSection(Section.ANSWER));
    }

    public List<Name> getNSTargets() {
        List<Name> targets = new ArrayList<>();
        for (Record r : message.getSection(Section.AUTHORITY)) {
            if (r instanceof NSRecord ns) {
                targets.add(ns.getTarget());
            }
        }
        return targets;
    }

    public List<NameServerAddress> getARecordAddresses(Name name) {
        List<NameServerAddress> result = new ArrayList<>();
        for (Record r : message.getSection(Section.ANSWER)) {
            if (r instanceof ARecord a && r.getName().equals(name)) {
                result.add(new NameServerAddress(a.getAddress().getHostAddress()));
            }
        }
        return result;
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
                    result.add(new NameServerAddress(a.getAddress().getHostAddress()));
                }
            }
        }
        return result;
    }

    public boolean isNoDataFor(Name qname, int qtype) {
        if (message.getRcode() != Rcode.NOERROR) {
            return false;
        }
        if (hasAnswer(qname, qtype)) {
            return false;
        }
        if (getCnameTarget(qname) != null) {
            return false;
        }
        return getNSTargets().isEmpty();
    }

    public Message toWireMessage() {
        return message;
    }

}
