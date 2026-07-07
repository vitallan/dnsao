package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveMixedAAndAaaaReferralFixture;
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
public class RecursiveMixedAAndAaaaReferralTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "mixed-glue.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String IPV4_NAMESERVER_HOST = "ns1.mixed-glue.com";
    private static final String IPV4_NAMESERVER_IP = "127.0.0.60";
    private static final String IPV6_NAMESERVER_HOST = "ns2.mixed-glue.com";
    private static final String IPV6_NAMESERVER_IP = "2001:db8::60";
    private static final String FINAL_IP = "10.0.0.18";
    private static final long REFERRAL_TTL = 300;

    private FakeServer silentIpv4Server;
    private FakeServer ipv6DelegatedServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        silentIpv4Server = startSilentFakeUdpServer();
        trackExtraFakeServer(silentIpv4Server);
        ipv6DelegatedServer = startFakeUdpServer();
        trackExtraFakeServer(ipv6DelegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(IPV4_NAMESERVER_IP, silentIpv4Server.getPort());
        stepResolverFactory.setRoute(IPV6_NAMESERVER_IP, ipv6DelegatedServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveMixedAAndAaaaReferralFixture fixture = new RecursiveMixedAAndAaaaReferralFixture(fakeUpstreamServer, ipv6DelegatedServer);
        expectedHistories = fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IPV4,
                IPV4_NAMESERVER_HOST,
                IPV4_NAMESERVER_IP,
                IPV6_NAMESERVER_HOST,
                IPV6_NAMESERVER_IP,
                FINAL_IP,
                REFERRAL_TTL
        );
    }

    @Test
    public void fallsBackFromAReferralServerToAaaaReferralServer() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(silentIpv4Server, expectedHistories.secondaryQueries());
        assertReceivedQueries(ipv6DelegatedServer, expectedHistories.tertiaryQueries());
    }
}
