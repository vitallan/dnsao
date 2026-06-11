package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveTcpFallbackFinalAnswerFixture;
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
public class RecursiveTcpFallbackFinalAnswerTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "tcp-final.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String TRANSPORT_NAMESERVER_HOST = "ns1.tcp-final.com";
    private static final String TRANSPORT_NAMESERVER_IP = "127.0.0.90";
    private static final String FINAL_IP = "10.0.0.19";
    private static final long REFERRAL_TTL = 300;

    private FakeUdpTcpDnsServer transportServer;
    private RecursiveTransportServerHistories expectedHistories;

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.OFF;
    }

    @Override
    protected void beforeServerStart() throws Exception {
        transportServer = startFakeUdpTcpDnsServer();
        trackExtraFakeServer(transportServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(TRANSPORT_NAMESERVER_IP, transportServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveTcpFallbackFinalAnswerFixture fixture = new RecursiveTcpFallbackFinalAnswerFixture(fakeUpstreamServer, transportServer);
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
    public void retriesFinalAnswerOverTcpWhenUdpStepIsTruncated() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedUdpQueries(transportServer, expectedHistories.secondaryUdpQueries());
        assertReceivedTcpQueries(transportServer, expectedHistories.secondaryTcpQueries());
    }
}
