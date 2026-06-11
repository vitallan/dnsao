package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveDoDowngradeFixture;
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
public abstract class AbstractRecursiveDoDowngradeTest extends AbstractRecursiveDnssecPhaseThreeTest {

    protected static final String DOMAIN = "phase3-downgrade.com";
    protected static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    protected static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    protected static final String TRANSPORT_NAMESERVER_HOST = "ns1.phase3-downgrade.com";
    protected static final String TRANSPORT_NAMESERVER_IP = "127.0.0.93";
    protected static final String FINAL_IP = "10.0.0.23";
    protected static final long REFERRAL_TTL = 300;

    protected FakeServer transportServer;
    protected RecursiveServerHistories expectedHistories;

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @Override
    protected void beforeServerStart() throws Exception {
        transportServer = startFakeUdpServer();
        trackExtraFakeServer(transportServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(TRANSPORT_NAMESERVER_IP, transportServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveDoDowngradeFixture fixture = new RecursiveDoDowngradeFixture(fakeUpstreamServer, transportServer);
        expectedHistories = fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IPV4,
                TRANSPORT_NAMESERVER_HOST,
                TRANSPORT_NAMESERVER_IP,
                FINAL_IP,
                REFERRAL_TTL,
                downgradeTriggerRcode()
        );
    }

    protected abstract int downgradeTriggerRcode();

    @Test
    public void retriesSameStepWithoutDoAfterCompatibleDowngradeSignal() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(transportServer, expectedHistories.secondaryQueries());
        assertDoValues(getReceivedMessages(fakeUpstreamServer), true, true);
        assertDoValues(getReceivedMessages(transportServer), true, false);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
    }
}
