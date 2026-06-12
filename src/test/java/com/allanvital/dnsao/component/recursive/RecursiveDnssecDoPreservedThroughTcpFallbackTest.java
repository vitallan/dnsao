package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveTcpFallbackFinalAnswerFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveTransportServerHistories;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeUdpTcpDnsServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveDnssecDoPreservedThroughTcpFallbackTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "dnssec-tcp-test.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String TRANSPORT_NAMESERVER_HOST = "ns1.dnssec-tcp-test.com";
    private static final String TRANSPORT_NAMESERVER_IP = "127.0.0.92";
    private static final String FINAL_IP = "10.0.0.22";
    private static final long REFERRAL_TTL = 300;

    private FakeUdpTcpDnsServer transportServer;
    private RecursiveTransportServerHistories expectedHistories;

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @Override
    protected void beforeServerStart() throws Exception {
        transportServer = startFakeUdpTcpDnsServer();
        trackExtraFakeServer(transportServer);
        fakeUpstreamServer = startFakeUdpServer();
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
    public void doBitPreservedAcrossUdpToTcpFallback() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);
        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedUdpQueries(transportServer, expectedHistories.secondaryUdpQueries());
        assertReceivedTcpQueries(transportServer, expectedHistories.secondaryTcpQueries());

        for (Message msg : getReceivedMessages(fakeUpstreamServer)) {
            OPTRecord opt = MessageHelper.getOpt(msg);
            assertTrue(MessageHelper.isDO(opt), "DO must be set on bootstrap server query");
        }
        for (Message msg : transportServer.getReceivedMessages()) {
            OPTRecord opt = MessageHelper.getOpt(msg);
            assertTrue(MessageHelper.isDO(opt), "DO must be set on transport server query");
        }
    }

}
