package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecursiveConcurrentRaceDownssecDowngradeAfterRaceTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.7";
    private static final String FAST_REFUSED_ROOT_IP = "127.0.0.110";
    private static final String SLOW_SUCCESS_ROOT_IP = "127.0.0.111";
    private static final long REFERRAL_TTL = 300;
    private static final long SLOW_DELAY_MS = 2000;

    private FakeServer fastRefusedRootServer;
    private FakeServer slowSuccessRootServer;

    @Override
    protected void beforeServerStart() throws Exception {
        fastRefusedRootServer = new FakeUdpServer(0);
        fastRefusedRootServer.start();
        trackExtraFakeServer(fastRefusedRootServer);

        slowSuccessRootServer = new DelayedFakeUdpServer(0, SLOW_DELAY_MS);
        slowSuccessRootServer.start();
        trackExtraFakeServer(slowSuccessRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(FAST_REFUSED_ROOT_IP, fastRefusedRootServer.getPort()),
                new NameServerAddress(SLOW_SUCCESS_ROOT_IP, slowSuccessRootServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FAST_REFUSED_ROOT_IP, fastRefusedRootServer.getPort());
        stepResolverFactory.setRoute(SLOW_SUCCESS_ROOT_IP, slowSuccessRootServer.getPort());
        stepResolverFactory.setRoute(NS_IP, fakeUpstreamServer.getPort());
    }

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @BeforeEach
    public void loadScenario() {
        RacingFixtureHelper helper = new RacingFixtureHelper(fakeUpstreamServer);

        Message comNsQuery = RacingFixtureHelper.buildQuery("com", Type.NS);
        Message domainNsQuery = RacingFixtureHelper.buildQuery(DOMAIN, Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        Message refusedResponse = MessageHelper.buildRefused(comNsQuery);
        Message correctNsReferral = helper.buildNsReferralWithGlueResponse(comNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainNsReferral = helper.buildNsReferralWithGlueResponse(domainNsQuery, NS_HOST, NS_IP, REFERRAL_TTL);
        Message domainAResponse = MessageHelper.buildAResponse(domainAQuery, FINAL_IP, REFERRAL_TTL);

        fastRefusedRootServer.mockResponseChain(comNsQuery, refusedResponse, correctNsReferral);
        slowSuccessRootServer.mockResponse(comNsQuery, correctNsReferral);
        fastRefusedRootServer.mockResponse(domainNsQuery, domainNsReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainAResponse);
    }

    @Test
    public void dnssecDowngradeAfterRaceReturnsCorrectAnswer() throws IOException {
        fastRefusedRootServer.clearReceivedQueries();

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        assertTrue(fastRefusedRootServer.getReceivedMessages().size() >= 2,
                "Fast server should have received 2 queries (original with DO + downgraded without DO)");
    }
}
