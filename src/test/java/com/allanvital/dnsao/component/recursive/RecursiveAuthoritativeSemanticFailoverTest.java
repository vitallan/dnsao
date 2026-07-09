package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveAuthoritativeSemanticFailoverFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.DelayedFakeUdpServer;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthoritativeSemanticFailoverTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "semantic-failover.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final String FIRST_AUTH_NS_HOST = "ns1.semantic-failover.com";
    private static final String FIRST_AUTH_NS_IP = "127.0.0.101";
    private static final String SECOND_AUTH_NS_HOST = "ns2.semantic-failover.com";
    private static final String SECOND_AUTH_NS_IP = "127.0.0.102";
    private static final String HELPER_TARGET_HOST = "ns1.loop-helper.net";
    private static final String HELPER_ZONE = "loop-helper.net";
    private static final String HELPER_ZONE_BOOTSTRAP_HOST = "ns-root.loop-helper.net";
    private static final String HELPER_ZONE_BOOTSTRAP_IP = "127.0.0.103";
    private static final String LOOP_NS_A_HOST = "ns-a.loop-helper.net";
    private static final String LOOP_NS_A_IP = "127.0.0.104";
    private static final String LOOP_NS_B_HOST = "ns-b.loop-helper.net";
    private static final String LOOP_NS_B_IP = "127.0.0.105";
    private static final String FINAL_IP = "10.0.0.91";
    private static final long TTL = 300;
    private static final long SECOND_AUTH_DELAY_MS = 250L;

    private FakeServer firstAuthoritativeServer;
    private FakeServer secondAuthoritativeServer;
    private FakeServer loopServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        firstAuthoritativeServer = startFakeUdpServer();
        trackExtraFakeServer(firstAuthoritativeServer);
        secondAuthoritativeServer = new DelayedFakeUdpServer(0, SECOND_AUTH_DELAY_MS);
        secondAuthoritativeServer.start();
        trackExtraFakeServer(secondAuthoritativeServer);
        loopServer = startFakeUdpServer();
        trackExtraFakeServer(loopServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_AUTH_NS_IP, firstAuthoritativeServer.getPort());
        stepResolverFactory.setRoute(SECOND_AUTH_NS_IP, secondAuthoritativeServer.getPort());
        stepResolverFactory.setRoute(HELPER_ZONE_BOOTSTRAP_IP, loopServer.getPort());
        stepResolverFactory.setRoute(LOOP_NS_A_IP, loopServer.getPort());
        stepResolverFactory.setRoute(LOOP_NS_B_IP, loopServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveAuthoritativeSemanticFailoverFixture fixture = new RecursiveAuthoritativeSemanticFailoverFixture(
                fakeUpstreamServer,
                firstAuthoritativeServer,
                secondAuthoritativeServer,
                loopServer
        );
        expectedHistories = fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                FIRST_AUTH_NS_HOST,
                FIRST_AUTH_NS_IP,
                SECOND_AUTH_NS_HOST,
                SECOND_AUTH_NS_IP,
                HELPER_TARGET_HOST,
                HELPER_ZONE,
                HELPER_ZONE_BOOTSTRAP_HOST,
                HELPER_ZONE_BOOTSTRAP_IP,
                LOOP_NS_A_HOST,
                LOOP_NS_A_IP,
                LOOP_NS_B_HOST,
                LOOP_NS_B_IP,
                FINAL_IP,
                TTL
        );
    }

    @Test
    public void fallsBackToSecondAuthoritativeServerWhenFirstResponseIsSemanticallyUnusable() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        List<com.allanvital.dnsao.graph.bean.DnsQueryKey> upstreamQueries = fakeUpstreamServer.getReceivedQueries();
        assertTrue(upstreamQueries.contains(key("com", Type.NS)));
        assertTrue(upstreamQueries.contains(key(DOMAIN, Type.NS)));

        assertTrue(firstAuthoritativeServer.getReceivedQueries().contains(key(DOMAIN, Type.A)));
        assertTrue(secondAuthoritativeServer.getReceivedQueries().contains(key(DOMAIN, Type.A)));

        List<com.allanvital.dnsao.graph.bean.DnsQueryKey> loopQueries = loopServer.getReceivedQueries();
        assertTrue(loopQueries.contains(key(HELPER_TARGET_HOST, Type.A)));
    }

    private com.allanvital.dnsao.graph.bean.DnsQueryKey key(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return new com.allanvital.dnsao.graph.bean.DnsQueryKey(Name.fromString(normalized), qtype, DClass.IN);
        } catch (TextParseException e) {
            fail("failed to build expected query key: " + e.getMessage());
            return null;
        }
    }
}
