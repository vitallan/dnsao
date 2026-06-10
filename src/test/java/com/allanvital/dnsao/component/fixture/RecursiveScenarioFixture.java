package com.allanvital.dnsao.component.fixture;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveScenarioFixture {

    private static final long DEFAULT_FINAL_TTL = 60;

    private final FakeServer fakeServer;

    public RecursiveScenarioFixture(FakeServer fakeServer) {
        this.fakeServer = fakeServer;
    }

    public List<DnsQueryKey> loadMinifiedHappyPath(String domain, String finalIp, String nsHost, String nsIp, long referralTtl) {
        fakeServer.clearReceivedQueries();

        // discover who serves .com.
        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        // discover who serves allanvital.com.
        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        // Final data lookup once the authoritative servers are known.
        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, DEFAULT_FINAL_TTL));

        return List.of(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A)
        );
    }

    public List<DnsQueryKey> loadMinifiedServfailPath(String domain, String nsHost, String nsIp, long referralTtl) {
        fakeServer.clearReceivedQueries();

        // discover who serves .com.
        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        // Zone discovery step for the target name; we intentionally omit the final A answer.
        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        return List.of(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A)
        );
    }

    public List<DnsQueryKey> loadMinifiedNsWithoutGlue(String domain, String nsHost, String nsIp, String finalIp, long referralTtl) {
        fakeServer.clearReceivedQueries();

        // discover who serves .com.
        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        // Delegation for the target name without glue; this is the scenario under test.
        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, MessageHelper.buildNsReferralResponse(domainNsQuery, nsHost, referralTtl));

        // Minimized helper-name resolution starts from the TLD again for ns1.com.
        Message nsNsQuery = buildRequest(nsHost, Type.NS);
        fakeServer.mockResponse(nsNsQuery, buildNsReferralWithGlueResponse(nsNsQuery, nsHost, nsIp, referralTtl));

        // Final address lookup for the helper nameserver hostname.
        Message nsAQuery = MessageHelper.buildARequest(nsHost);
        fakeServer.mockResponse(nsAQuery, MessageHelper.buildAResponse(nsAQuery, nsIp, referralTtl));

        // Original data lookup retried against the resolved authoritative server.
        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return List.of(
                key("com", Type.NS),
                key(domain, Type.NS),
                key("com", Type.NS),
                key(nsHost, Type.NS),
                key(nsHost, Type.A),
                key(domain, Type.A)
        );
    }

    private Message buildRequest(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return Message.newQuery(Record.newRecord(Name.fromString(normalized), qtype, DClass.IN));
        } catch (TextParseException e) {
            fail("failed to create request: " + e.getMessage());
            return null;
        }
    }

    private Message buildNsReferralWithGlueResponse(Message request, String nsHost, String nsIp, long ttl) {
        try {
            Message response = MessageHelper.buildNsReferralResponse(request, nsHost, ttl);
            String normalizedNsHost = nsHost.endsWith(".") ? nsHost : nsHost + ".";
            Name nsName = Name.fromString(normalizedNsHost);
            ARecord glue = new ARecord(nsName, DClass.IN, ttl, InetAddress.getByName(nsIp));
            response.addRecord(glue, Section.ADDITIONAL);
            return response;
        } catch (IOException e) {
            fail("failed to create referral with glue: " + e.getMessage());
            return null;
        }
    }

    private DnsQueryKey key(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return new DnsQueryKey(Name.fromString(normalized), qtype, DClass.IN);
        } catch (TextParseException e) {
            fail("failed to build expected query key: " + e.getMessage());
            return null;
        }
    }
}
