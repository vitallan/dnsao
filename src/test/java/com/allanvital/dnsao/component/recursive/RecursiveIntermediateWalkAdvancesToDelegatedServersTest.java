package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveIntermediateWalkAdvancesToDelegatedServersFixture;
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

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveIntermediateWalkAdvancesToDelegatedServersTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String TLD_NAMESERVER_HOST = "a.gtld-servers.net";
    private static final String TLD_NAMESERVER_IP = "127.0.0.121";
    private static final String AUTHORITATIVE_NAMESERVER_HOST = "ns1.allanvital.com";
    private static final String AUTHORITATIVE_NAMESERVER_IP = "127.0.0.122";
    private static final String FINAL_IP = "10.0.0.121";
    private static final long TTL = 300;

    private FakeServer tldServer;
    private FakeServer authoritativeServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        tldServer = startFakeUdpServer();
        trackExtraFakeServer(tldServer);
        authoritativeServer = startFakeUdpServer();
        trackExtraFakeServer(authoritativeServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(TLD_NAMESERVER_IP, tldServer.getPort());
        stepResolverFactory.setRoute(AUTHORITATIVE_NAMESERVER_IP, authoritativeServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveIntermediateWalkAdvancesToDelegatedServersFixture fixture = new RecursiveIntermediateWalkAdvancesToDelegatedServersFixture(fakeUpstreamServer, tldServer, authoritativeServer);
        expectedHistories = fixture.load(
                DOMAIN,
                TLD_NAMESERVER_HOST,
                TLD_NAMESERVER_IP,
                AUTHORITATIVE_NAMESERVER_HOST,
                AUTHORITATIVE_NAMESERVER_IP,
                FINAL_IP,
                TTL
        );
    }

    @Test
    public void walkUsesDelegatedTldServersForDeeperNamesInsteadOfRequeryingRoot() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(tldServer, expectedHistories.secondaryQueries());
        assertReceivedQueries(authoritativeServer, expectedHistories.tertiaryQueries());
    }
}
