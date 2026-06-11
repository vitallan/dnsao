package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractRecursiveScenarioFixture {

    protected final FakeServer fakeServer;

    protected AbstractRecursiveScenarioFixture(FakeServer fakeServer) {
        this.fakeServer = fakeServer;
    }

    protected Message buildRequest(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return Message.newQuery(Record.newRecord(Name.fromString(normalized), qtype, DClass.IN));
        } catch (TextParseException e) {
            fail("failed to create request: " + e.getMessage());
            return null;
        }
    }

    protected Message buildNsReferralWithGlueResponse(Message request, String nsHost, String nsIp, long ttl) {
        return buildNsReferralWithGluesResponse(request, List.of(nsHost), List.of(nsIp), ttl);
    }

    protected Message buildNsReferralWithGluesResponse(Message request, List<String> nsHosts, List<String> nsIps, long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            Record question = request.getQuestion();
            if (question == null) {
                return response;
            }
            List<Name> nsNames = new ArrayList<>();
            for (String nsHost : nsHosts) {
                String normalizedNsHost = nsHost.endsWith(".") ? nsHost : nsHost + ".";
                Name nsName = Name.fromString(normalizedNsHost);
                nsNames.add(nsName);
                response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, nsName), Section.AUTHORITY);
            }
            for (int i = 0; i < nsNames.size(); i++) {
                ARecord glue = new ARecord(nsNames.get(i), DClass.IN, ttl, InetAddress.getByName(nsIps.get(i)));
                response.addRecord(glue, Section.ADDITIONAL);
            }
            return response;
        } catch (IOException e) {
            fail("failed to create referral with glue: " + e.getMessage());
            return null;
        }
    }

    protected Message buildNoDataResponse(Message request) {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setOpcode(request.getHeader().getOpcode());
        response.getHeader().setFlag(Flags.QR);
        if (request.getHeader().getFlag(Flags.RD)) {
            response.getHeader().setFlag(Flags.RD);
        }
        if (request.getQuestion() != null) {
            response.addRecord(request.getQuestion(), Section.QUESTION);
        }
        response.getHeader().setRcode(Rcode.NOERROR);
        return response;
    }

    protected Message buildCnameResponse(Message request, String target, long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            Record question = request.getQuestion();
            if (question != null) {
                String normalizedTarget = target.endsWith(".") ? target : target + ".";
                CNAMERecord cnameRecord = new CNAMERecord(question.getName(), question.getDClass(), ttl, Name.fromString(normalizedTarget));
                response.addRecord(cnameRecord, Section.ANSWER);
            }
            return response;
        } catch (TextParseException e) {
            fail("failed to create CNAME response: " + e.getMessage());
            return null;
        }
    }

    protected DnsQueryKey key(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return new DnsQueryKey(Name.fromString(normalized), qtype, DClass.IN);
        } catch (TextParseException e) {
            fail("failed to build expected query key: " + e.getMessage());
            return null;
        }
    }

    protected void clearHistory() {
        fakeServer.clearReceivedQueries();
    }

    protected List<DnsQueryKey> history(DnsQueryKey... queries) {
        return List.of(queries);
    }

    protected List<DnsQueryKey> mergeHistories(List<DnsQueryKey>... histories) {
        List<DnsQueryKey> combined = new ArrayList<>();
        for (List<DnsQueryKey> history : histories) {
            combined.addAll(history);
        }
        return List.copyOf(combined);
    }

    protected List<DnsQueryKey> repeat(DnsQueryKey query, int times) {
        List<DnsQueryKey> repeated = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            repeated.add(query);
        }
        return List.copyOf(repeated);
    }
}
