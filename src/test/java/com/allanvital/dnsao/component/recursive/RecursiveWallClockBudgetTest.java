package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveWallClockBudgetFixture;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.DelayedFakeUdpServer;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveWallClockBudgetTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String ROOT_IP = "127.0.0.120";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.121";
    private static final String AUTHORITATIVE_NS_HOST = "ns1.allanvital.com";
    private static final String AUTHORITATIVE_NS_IP = "127.0.0.122";
    private static final String FINAL_IP = "10.0.0.41";
    private static final long REFERRAL_TTL = 300;
    private static final long ROOT_DELAY_MS = 150L;
    private static final int MAX_HELPER_RESOLUTIONS = 8;
    private static final long MAX_SESSION_ELAPSED_MS = 250L;

    private FakeServer delayedRootServer;

    @Override
    protected void beforeServerStart() throws Exception {
        assembler.setRecursiveBudgetOverrides(MAX_HELPER_RESOLUTIONS, MAX_SESSION_ELAPSED_MS);
        delayedRootServer = new DelayedFakeUdpServer(0, ROOT_DELAY_MS);
        delayedRootServer.start();
        trackExtraFakeServer(delayedRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(new NameServerAddress(ROOT_IP, delayedRootServer.getPort()));
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(ROOT_IP, delayedRootServer.getPort());
        stepResolverFactory.setRoute(AUTHORITATIVE_NS_IP, fakeUpstreamServer.getPort());
    }

    @Test
    public void wallClockBudgetStopsResolutionBeforeFinalQuery() throws IOException {
        RecursiveWallClockBudgetFixture fixture = new RecursiveWallClockBudgetFixture(delayedRootServer, fakeUpstreamServer);
        RecursiveServerHistories expectedHistories = fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                AUTHORITATIVE_NS_HOST,
                AUTHORITATIVE_NS_IP,
                FINAL_IP,
                REFERRAL_TTL
        );

        Instant start = Instant.now();
        Message response = executeRequestOnOwnServer(DOMAIN);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertTrue(elapsedMs < 450, "resolution should stop before the full delayed chain completes; elapsed=" + elapsedMs + "ms");
        assertReceivedQueries(delayedRootServer, expectedHistories.primaryQueries());
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.secondaryQueries());
    }
}
