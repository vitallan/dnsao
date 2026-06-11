package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveMixedAAndAaaaReferralFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer ipv6DelegatedServer;

    public RecursiveMixedAAndAaaaReferralFixture(FakeServer fakeServer, FakeServer ipv6DelegatedServer) {
        super(fakeServer);
        this.ipv6DelegatedServer = ipv6DelegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String bootstrapNsHost,
                                         String bootstrapNsIpv4,
                                         String ipv4NameserverHost,
                                         String ipv4NameserverIp,
                                         String ipv6NameserverHost,
                                         String ipv6NameserverIp,
                                         String finalIp,
                                         long referralTtl) {
        clearHistory();
        ipv6DelegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIpv4, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildMixedReferral(domainNsQuery, ipv4NameserverHost, ipv4NameserverIp, ipv6NameserverHost, ipv6NameserverIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        ipv6DelegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A)),
                history(key(domain, Type.A))
        );
    }

    private Message buildMixedReferral(Message request,
                                       String ipv4NameserverHost,
                                       String ipv4NameserverIp,
                                       String ipv6NameserverHost,
                                       String ipv6NameserverIp,
                                       long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            Record question = request.getQuestion();
            if (question == null) {
                return response;
            }

            Name ipv4Name = Name.fromString(ipv4NameserverHost.endsWith(".") ? ipv4NameserverHost : ipv4NameserverHost + ".");
            Name ipv6Name = Name.fromString(ipv6NameserverHost.endsWith(".") ? ipv6NameserverHost : ipv6NameserverHost + ".");

            response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, ipv4Name), Section.AUTHORITY);
            response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, ipv6Name), Section.AUTHORITY);
            response.addRecord(new ARecord(ipv4Name, DClass.IN, ttl, InetAddress.getByName(ipv4NameserverIp)), Section.ADDITIONAL);
            response.addRecord(new AAAARecord(ipv6Name, DClass.IN, ttl, InetAddress.getByName(ipv6NameserverIp)), Section.ADDITIONAL);
            return response;
        } catch (IOException e) {
            fail("failed to create mixed A/AAAA referral: " + e.getMessage());
            return null;
        }
    }
}
