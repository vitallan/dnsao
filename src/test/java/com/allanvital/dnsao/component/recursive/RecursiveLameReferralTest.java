package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveLameReferralFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
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
public class RecursiveLameReferralTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "lame-referral.com";
    private static final String DELEGATED_NAMESERVER_HOST = "ns1.lame.com";
    private static final String DELEGATED_NAMESERVER_IP = "127.0.0.50";
    private static final String LAME_NAMESERVER_HOST = "ns2.lame-helper.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private FakeServer delegatedServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(DELEGATED_NAMESERVER_IP, delegatedServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveLameReferralFixture fixture = new RecursiveLameReferralFixture(fakeUpstreamServer, delegatedServer);
        expectedHistories = fixture.load(
                DOMAIN,
                DELEGATED_NAMESERVER_HOST,
                DELEGATED_NAMESERVER_IP,
                LAME_NAMESERVER_HOST,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                REFERRAL_TTL
        );
    }

    @Test
    public void failsCleanlyWhenDelegatedServerReturnsLameReferral() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());
    }
}
