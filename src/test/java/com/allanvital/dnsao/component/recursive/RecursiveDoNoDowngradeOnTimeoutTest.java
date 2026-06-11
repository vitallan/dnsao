package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveNameserverFailoverFixture;
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
public class RecursiveDoNoDowngradeOnTimeoutTest extends AbstractRecursiveDnssecPhaseThreeTest {

    private static final String DOMAIN = "phase3-timeout.com";
    private static final String FIRST_NS_HOST = "ns1.phase3-timeout.com";
    private static final String FIRST_NS_IP = "127.0.0.94";
    private static final String SECOND_NS_HOST = "ns2.phase3-timeout.com";
    private static final String SECOND_NS_IP = "127.0.0.95";
    private static final String FINAL_IP = "10.0.0.24";
    private static final long REFERRAL_TTL = 300;

    private FakeServer firstNameserver;
    private FakeServer secondNameserver;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @Override
    protected void beforeServerStart() throws Exception {
        firstNameserver = startSilentFakeUdpServer();
        trackExtraFakeServer(firstNameserver);
        secondNameserver = startFakeUdpServer();
        trackExtraFakeServer(secondNameserver);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_NS_IP, firstNameserver.getPort());
        stepResolverFactory.setRoute(SECOND_NS_IP, secondNameserver.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveNameserverFailoverFixture fixture = new RecursiveNameserverFailoverFixture(fakeUpstreamServer, secondNameserver);
        expectedHistories = fixture.load(DOMAIN, FIRST_NS_HOST, FIRST_NS_IP, SECOND_NS_HOST, SECOND_NS_IP, FINAL_IP, REFERRAL_TTL);
    }

    @Test
    public void keepsDoEnabledDuringNormalFailoverAfterTimeout() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(firstNameserver, expectedHistories.secondaryQueries());
        assertReceivedQueries(secondNameserver, expectedHistories.tertiaryQueries());
        assertDoValues(getReceivedMessages(fakeUpstreamServer), true, true);
        assertDoValues(getReceivedMessages(firstNameserver), true);
        assertDoValues(getReceivedMessages(secondNameserver), true);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
    }
}
