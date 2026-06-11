package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.dns.recursive.NameServerAddress;
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

public class RecursiveConcurrentRaceSilentServerFallbackTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.2";
    private static final String SILENT_ROOT_IP = "127.0.0.102";
    private static final String RESPONDING_ROOT_IP = "127.0.0.103";
    private static final long REFERRAL_TTL = 300;

    private FakeServer silentRootServer;
    private FakeServer respondingRootServer;

    @Override
    protected void beforeServerStart() throws Exception {
        silentRootServer = startSilentFakeUdpServer();
        trackExtraFakeServer(silentRootServer);

        respondingRootServer = new FakeUdpServer(0);
        respondingRootServer.start();
        trackExtraFakeServer(respondingRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(SILENT_ROOT_IP, silentRootServer.getPort()),
                new NameServerAddress(RESPONDING_ROOT_IP, respondingRootServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(SILENT_ROOT_IP, silentRootServer.getPort());
        stepResolverFactory.setRoute(RESPONDING_ROOT_IP, respondingRootServer.getPort());
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

        respondingRootServer.mockResponse(comNsQuery, nsReferral);
        respondingRootServer.mockResponse(domainNsQuery, domainNsReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainAResponse);
    }

    @Test
    public void silentServerDoesNotBlockRespondingServer() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
    }
}
