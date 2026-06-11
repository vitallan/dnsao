package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveTcpFallbackReferralFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveTransportServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeUdpTcpDnsServer;
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
public class RecursiveTcpFallbackReferralTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "tcp-referral.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String TRANSPORT_NAMESERVER_HOST = "ns1.tcp-referral.com";
    private static final String TRANSPORT_NAMESERVER_IP = "127.0.0.91";
    private static final String FINAL_IP = "10.0.0.20";
    private static final long REFERRAL_TTL = 300;

    private FakeUdpTcpDnsServer transportServer;
    private RecursiveTransportServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        transportServer = startFakeUdpTcpDnsServer();
        fakeUpstreamServer = transportServer;
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(TRANSPORT_NAMESERVER_IP, transportServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveTcpFallbackReferralFixture fixture = new RecursiveTcpFallbackReferralFixture(fakeUpstreamServer, transportServer);
        expectedHistories = fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IPV4,
                TRANSPORT_NAMESERVER_HOST,
                TRANSPORT_NAMESERVER_IP,
                FINAL_IP,
                REFERRAL_TTL
        );
    }

    @Test
    public void retriesReferralDiscoveryOverTcpWhenUdpNsStepIsTruncated() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedUdpQueries(transportServer, expectedHistories.secondaryUdpQueries());
        assertReceivedTcpQueries(transportServer, expectedHistories.secondaryTcpQueries());
    }
}
