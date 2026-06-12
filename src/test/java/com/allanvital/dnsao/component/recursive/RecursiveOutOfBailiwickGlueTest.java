package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.AbstractRecursiveScenarioFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveOutOfBailiwickGlueTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_TARGET = "ns1.evil.net";
    private static final String BAD_GLUE_IP = "127.0.0.23";
    private static final String REAL_NS_IP = "127.0.0.24";
    private static final String FINAL_IP = "10.0.0.44";
    private static final String BAD_FINAL_IP = "10.0.0.45";
    private static final long TTL = 300;

    private FakeServer badGlueServer;
    private FakeServer realAuthorityServer;

    @Override
    public void beforeServerStart() throws Exception {
        badGlueServer = new FakeUdpServer(0);
        badGlueServer.start();
        trackExtraFakeServer(badGlueServer);

        realAuthorityServer = new FakeUdpServer(0);
        realAuthorityServer.start();
        trackExtraFakeServer(realAuthorityServer);
    }

    @Override
    public void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(BAD_GLUE_IP, badGlueServer.getPort());
        stepResolverFactory.setRoute(REAL_NS_IP, realAuthorityServer.getPort());
    }

    @BeforeEach
    public void loadScenario() throws Exception {
        BailiwickFixtureHelper fixture = new BailiwickFixtureHelper(fakeUpstreamServer);

        Message domainNsQuery = fixture.request(DOMAIN, Type.NS);
        Message comNsQuery = fixture.request("com", Type.NS);
        Message netNsQuery = fixture.request("net", Type.NS);
        Message nsTargetNsQuery = fixture.request(NS_TARGET, Type.NS);
        Message nsTargetAQuery = fixture.request(NS_TARGET, Type.A);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        fakeUpstreamServer.mockResponse(comNsQuery, fixture.referralWithGlue(comNsQuery, "ns1.com", "127.0.0.21", TTL));
        fakeUpstreamServer.mockResponse(domainNsQuery, fixture.referralWithGlue(domainNsQuery, NS_TARGET, BAD_GLUE_IP, TTL));
        fakeUpstreamServer.mockResponse(netNsQuery, fixture.referralWithGlue(netNsQuery, "ns1.net", "127.0.0.22", TTL));
        fakeUpstreamServer.mockResponse(nsTargetNsQuery, fixture.referralWithGlue(nsTargetNsQuery, NS_TARGET, REAL_NS_IP, TTL));

        realAuthorityServer.mockResponse(nsTargetAQuery, MessageHelper.buildAResponse(nsTargetAQuery, REAL_NS_IP, TTL));
        realAuthorityServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL));
        badGlueServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, BAD_FINAL_IP, TTL));
    }

    @Test
    public void ignoresOutOfBailiwickGlueAndResolvesNameserverAddressRecursively() throws IOException {
        BailiwickFixtureHelper fixture = new BailiwickFixtureHelper(fakeUpstreamServer);

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(DOMAIN, Type.NS),
                fixture.key("net", Type.NS),
                fixture.key(NS_TARGET, Type.NS)
        ));
        assertReceivedQueries(realAuthorityServer, List.of(
                fixture.key(NS_TARGET, Type.A),
                fixture.key(DOMAIN, Type.A)
        ));
        assertReceivedQueries(badGlueServer, List.of());
    }

    private static final class BailiwickFixtureHelper extends AbstractRecursiveScenarioFixture {

        private BailiwickFixtureHelper(FakeServer fakeServer) {
            super(fakeServer);
        }

        public Message request(String qname, int qtype) {
            return super.buildRequest(qname, qtype);
        }

        public Message referralWithGlue(Message request, String nsHost, String nsIp, long ttl) {
            return super.buildNsReferralWithGlueResponse(request, nsHost, nsIp, ttl);
        }

        public DnsQueryKey key(String qname, int qtype) {
            return super.key(qname, qtype);
        }
    }
}
