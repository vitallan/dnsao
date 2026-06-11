package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.DelayedFakeUdpServer;
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

public class RecursiveConcurrentRaceFastestWinsTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.1";
    private static final String SLOW_ROOT_IP = "127.0.0.100";
    private static final String FAST_ROOT_IP = "127.0.0.101";
    private static final long REFERRAL_TTL = 300;
    private static final long SLOW_DELAY_MS = 2000;

    private FakeServer slowRootServer;
    private FakeServer fastRootServer;

    @Override
    protected void beforeServerStart() throws Exception {
        slowRootServer = new DelayedFakeUdpServer(0, SLOW_DELAY_MS);
        slowRootServer.start();
        trackExtraFakeServer(slowRootServer);

        fastRootServer = new FakeUdpServer(0);
        fastRootServer.start();
        trackExtraFakeServer(fastRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(SLOW_ROOT_IP, slowRootServer.getPort()),
                new NameServerAddress(FAST_ROOT_IP, fastRootServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(SLOW_ROOT_IP, slowRootServer.getPort());
        stepResolverFactory.setRoute(FAST_ROOT_IP, fastRootServer.getPort());
        stepResolverFactory.setRoute(NS_IP, fakeUpstreamServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RacingFixtureHelper helper = new RacingFixtureHelper(fakeUpstreamServer);

        Message comNsQuery = RacingFixtureHelper.buildQuery("com", Type.NS);
        Message domainNsQuery = RacingFixtureHelper.buildQuery(DOMAIN, Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        Message nsReferral = helper.buildNsReferralWithGlueResponse(comNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainNsReferral = helper.buildNsReferralWithGlueResponse(domainNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainAResponse = MessageHelper.buildAResponse(domainAQuery, FINAL_IP, REFERRAL_TTL);

        fastRootServer.mockResponse(comNsQuery, nsReferral);
        fastRootServer.mockResponse(domainNsQuery, domainNsReferral);
        slowRootServer.mockResponse(comNsQuery, nsReferral);
        slowRootServer.mockResponse(domainNsQuery, domainNsReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainAResponse);
    }

    @Test
    public void fastestRootHintWins() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
    }
}
