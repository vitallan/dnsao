package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveCnameToDeepSubdomainFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RecursiveCnameToDeepSubdomainResolvesCorrectlyTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "domain.com";
    private static final String CNAME_TARGET = "a.zone.deep.example.com";
    private static final String FINAL_IP = "10.0.0.42";
    private static final String COM_SERVER_IP = "127.0.0.131";
    private static final String DEEP_SERVER_IP = "127.0.0.132";
    private static final long TTL = 300;

    private FakeServer comServer;
    private FakeServer deepServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        comServer = startFakeUdpServer();
        trackExtraFakeServer(comServer);
        deepServer = startFakeUdpServer();
        trackExtraFakeServer(deepServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(COM_SERVER_IP, comServer.getPort());
        stepResolverFactory.setRoute(DEEP_SERVER_IP, deepServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveCnameToDeepSubdomainFixture fixture = new RecursiveCnameToDeepSubdomainFixture(
                fakeUpstreamServer, comServer, deepServer);
        expectedHistories = fixture.load(
                DOMAIN, CNAME_TARGET, FINAL_IP,
                COM_SERVER_IP, DEEP_SERVER_IP, TTL);
    }

    @Test
    public void followsCnameToDeepSubdomainThroughNonDelegationIntermediateNames() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(comServer, expectedHistories.secondaryQueries());
        assertReceivedQueries(deepServer, expectedHistories.tertiaryQueries());
    }
}
