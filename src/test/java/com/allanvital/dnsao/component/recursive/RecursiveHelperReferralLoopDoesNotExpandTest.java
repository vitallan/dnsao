package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveHelperReferralLoopDoesNotExpandFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHelperReferralLoopDoesNotExpandTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "loop-expansion.com";
    private static final String DELEGATED_NAMESERVER_HOST = "ns1.loop-expansion.com";
    private static final String DELEGATED_NAMESERVER_IP = "127.0.0.91";
    private static final String RESOLVED_AUTHORITATIVE_IP = "127.0.0.92";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final String HELPER_ZONE = "loop-helper.net";
    private static final String HELPER_ZONE_BOOTSTRAP_HOST = "ns-root.loop-helper.net";
    private static final String HELPER_ZONE_BOOTSTRAP_IP = "127.0.0.95";
    private static final String FIRST_HELPER_TARGET = "ns1.loop-helper.net";
    private static final String SECOND_HELPER_TARGET = "ns2.loop-helper.net";
    private static final String LOOP_NAMESERVER_A = "ns-a.loop-helper.net";
    private static final String LOOP_NAMESERVER_A_IP = "127.0.0.93";
    private static final String LOOP_NAMESERVER_B = "ns-b.loop-helper.net";
    private static final String LOOP_NAMESERVER_B_IP = "127.0.0.94";
    private static final String FINAL_IP = "10.0.0.88";
    private static final long TTL = 300;

    private FakeServer delegatedServer;
    private FakeServer loopServer;
    private RecursiveHelperReferralLoopDoesNotExpandFixture fixture;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
        loopServer = startFakeUdpServer();
        trackExtraFakeServer(loopServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(DELEGATED_NAMESERVER_IP, delegatedServer.getPort());
        stepResolverFactory.setRoute(RESOLVED_AUTHORITATIVE_IP, delegatedServer.getPort());
        stepResolverFactory.setRoute(HELPER_ZONE_BOOTSTRAP_IP, loopServer.getPort());
        stepResolverFactory.setRoute(LOOP_NAMESERVER_A_IP, loopServer.getPort());
        stepResolverFactory.setRoute(LOOP_NAMESERVER_B_IP, loopServer.getPort());
    }

    @BeforeEach
    public void setupFixture() {
        fixture = new RecursiveHelperReferralLoopDoesNotExpandFixture(fakeUpstreamServer, delegatedServer, loopServer);
    }

    @Test
    public void stopsHelperResolutionWhenReferralLoopIsDetectedInsteadOfExpandingNsTargets() throws IOException {
        RecursiveServerHistories expectedHistories = fixture.loadSingleLoopingHelper(
                DOMAIN,
                DELEGATED_NAMESERVER_HOST,
                DELEGATED_NAMESERVER_IP,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                FIRST_HELPER_TARGET,
                HELPER_ZONE,
                HELPER_ZONE_BOOTSTRAP_HOST,
                HELPER_ZONE_BOOTSTRAP_IP,
                LOOP_NAMESERVER_A,
                LOOP_NAMESERVER_A_IP,
                LOOP_NAMESERVER_B,
                LOOP_NAMESERVER_B_IP,
                TTL
        );

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());

        List<com.allanvital.dnsao.graph.bean.DnsQueryKey> loopQueries = loopServer.getReceivedQueries();
        assertTrue(loopQueries.contains(key(FIRST_HELPER_TARGET, Type.A)));
        assertFalse(loopQueries.contains(key(FIRST_HELPER_TARGET, Type.AAAA)));
        assertFalse(loopQueries.contains(key(SECOND_HELPER_TARGET, Type.A)));
    }

    @Test
    public void continuesToNextOriginalHelperTargetAfterLoopingHelperStops() throws IOException {
        RecursiveServerHistories expectedHistories = fixture.loadFallsBackToSecondHelperAfterLoop(
                DOMAIN,
                DELEGATED_NAMESERVER_HOST,
                DELEGATED_NAMESERVER_IP,
                RESOLVED_AUTHORITATIVE_IP,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                FIRST_HELPER_TARGET,
                SECOND_HELPER_TARGET,
                HELPER_ZONE,
                HELPER_ZONE_BOOTSTRAP_HOST,
                HELPER_ZONE_BOOTSTRAP_IP,
                LOOP_NAMESERVER_A,
                LOOP_NAMESERVER_A_IP,
                LOOP_NAMESERVER_B,
                LOOP_NAMESERVER_B_IP,
                FINAL_IP,
                TTL
        );

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());

        List<com.allanvital.dnsao.graph.bean.DnsQueryKey> loopQueries = loopServer.getReceivedQueries();
        assertTrue(loopQueries.contains(key(FIRST_HELPER_TARGET, Type.A)));
        assertFalse(loopQueries.contains(key(FIRST_HELPER_TARGET, Type.AAAA)));
        assertTrue(loopQueries.contains(key(SECOND_HELPER_TARGET, Type.A)));
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
