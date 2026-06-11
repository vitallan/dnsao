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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecursiveConcurrentRaceAllServersReceiveQueryTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.5";
    private static final String FIRST_ROOT_IP = "127.0.0.106";
    private static final String SECOND_ROOT_IP = "127.0.0.107";
    private static final long REFERRAL_TTL = 300;

    private FakeServer firstRootServer;
    private FakeServer secondRootServer;

    @Override
    protected void beforeServerStart() throws Exception {
        firstRootServer = new FakeUdpServer(0);
        firstRootServer.start();
        trackExtraFakeServer(firstRootServer);

        secondRootServer = new FakeUdpServer(0);
        secondRootServer.start();
        trackExtraFakeServer(secondRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(FIRST_ROOT_IP, firstRootServer.getPort()),
                new NameServerAddress(SECOND_ROOT_IP, secondRootServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_ROOT_IP, firstRootServer.getPort());
        stepResolverFactory.setRoute(SECOND_ROOT_IP, secondRootServer.getPort());
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

        firstRootServer.mockResponse(comNsQuery, nsReferral);
        firstRootServer.mockResponse(domainNsQuery, domainNsReferral);
        secondRootServer.mockResponse(comNsQuery, nsReferral);
        secondRootServer.mockResponse(domainNsQuery, domainNsReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainAResponse);
    }

    @Test
    public void allServersReceiveQuery() throws IOException {
        firstRootServer.clearReceivedQueries();
        secondRootServer.clearReceivedQueries();

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        assertFalse(firstRootServer.getReceivedQueries().isEmpty(),
                "First root hint server should have received at least one query");
        assertFalse(secondRootServer.getReceivedQueries().isEmpty(),
                "Second root hint server should have received at least one query");
    }
}
