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
public class RecursiveInBailiwickGlueStillWorksTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String IN_BAILIWICK_NS_HOST = "ns1.allanvital.com";
    private static final String IN_BAILIWICK_NS_IP = "127.0.0.32";
    private static final String FINAL_IP = "10.0.0.54";
    private static final long TTL = 300;

    private FakeServer authoritativeServer;

    @Override
    public void beforeServerStart() throws Exception {
        authoritativeServer = new FakeUdpServer(0);
        authoritativeServer.start();
        trackExtraFakeServer(authoritativeServer);
    }

    @Override
    public void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(IN_BAILIWICK_NS_IP, authoritativeServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        GlueFixtureHelper fixture = new GlueFixtureHelper(fakeUpstreamServer);

        Message domainNsQuery = fixture.request(DOMAIN, Type.NS);
        Message comNsQuery = fixture.request("com", Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        fakeUpstreamServer.mockResponse(comNsQuery, fixture.referralWithGlue(comNsQuery, "ns1.com", "127.0.0.31", TTL));
        fakeUpstreamServer.mockResponse(domainNsQuery, fixture.referralWithGlue(domainNsQuery, IN_BAILIWICK_NS_HOST, IN_BAILIWICK_NS_IP, TTL));
        authoritativeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL));
    }

    @Test
    public void keepsUsingInBailiwickGlueDirectly() throws IOException {
        GlueFixtureHelper fixture = new GlueFixtureHelper(fakeUpstreamServer);

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(DOMAIN, Type.NS)
        ));
        assertReceivedQueries(authoritativeServer, List.of(fixture.key(DOMAIN, Type.A)));
    }

    private static final class GlueFixtureHelper extends AbstractRecursiveScenarioFixture {

        private GlueFixtureHelper(FakeServer fakeServer) {
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
