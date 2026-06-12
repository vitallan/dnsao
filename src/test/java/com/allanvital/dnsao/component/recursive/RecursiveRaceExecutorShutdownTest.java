package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.DelayedFakeUdpServer;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveRaceExecutorShutdownTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.61";
    private static final String SLOW_ROOT_IP = "127.0.0.111";
    private static final String FAST_ROOT_IP = "127.0.0.112";
    private static final long TTL = 300;
    private static final long SLOW_DELAY_MS = 1500;
    private static final String THREAD_PREFIX = "dns-race-";

    private FakeServer slowRootServer;
    private FakeServer fastRootServer;

    @Override
    public void beforeServerStart() throws Exception {
        slowRootServer = new DelayedFakeUdpServer(0, SLOW_DELAY_MS);
        slowRootServer.start();
        trackExtraFakeServer(slowRootServer);

        fastRootServer = new FakeUdpServer(0);
        fastRootServer.start();
        trackExtraFakeServer(fastRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(SLOW_ROOT_IP, slowRootServer.getPort()),
                new NameServerAddress(FAST_ROOT_IP, fastRootServer.getPort())
        );
    }

    @Override
    public void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(SLOW_ROOT_IP, slowRootServer.getPort());
        stepResolverFactory.setRoute(FAST_ROOT_IP, fastRootServer.getPort());
        stepResolverFactory.setRoute(NS_IP, fakeUpstreamServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RacingFixtureHelper helper = new RacingFixtureHelper(fakeUpstreamServer);

        Message comNsQuery = RacingFixtureHelper.buildQuery("com", Type.NS);
        Message domainNsQuery = RacingFixtureHelper.buildQuery(DOMAIN, Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        Message rootReferral = helper.buildNsReferralWithGlueResponse(comNsQuery, NS_HOST, NS_IP, TTL);
        Message domainReferral = helper.buildNsReferralWithGlueResponse(domainNsQuery, NS_HOST, NS_IP, TTL);
        Message domainResponse = MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL);

        slowRootServer.mockResponse(comNsQuery, rootReferral);
        slowRootServer.mockResponse(domainNsQuery, domainReferral);
        fastRootServer.mockResponse(comNsQuery, rootReferral);
        fastRootServer.mockResponse(domainNsQuery, domainReferral);
        fakeUpstreamServer.mockResponse(domainAQuery, domainResponse);
    }

    @Test
    public void dnsServerStopShutsDownRecursiveRaceThreads() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertTrue(waitForRaceThreadCountAtLeast(1, 1000), "Expected at least one dns-race thread to exist after racing authorities");

        dnsServer.stop();

        assertTrue(waitForRaceThreadCountAtMost(0, 500), "Expected dns-race threads to stop when the DNS server stops");
    }

    private boolean waitForRaceThreadCountAtLeast(int minimumCount, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (countThreadsByPrefix(THREAD_PREFIX) >= minimumCount) {
                return true;
            }
            sleep(10);
        }
        return countThreadsByPrefix(THREAD_PREFIX) >= minimumCount;
    }

    private boolean waitForRaceThreadCountAtMost(int maximumCount, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (countThreadsByPrefix(THREAD_PREFIX) <= maximumCount) {
                return true;
            }
            sleep(10);
        }
        return countThreadsByPrefix(THREAD_PREFIX) <= maximumCount;
    }

    private int countThreadsByPrefix(String prefix) {
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        int count = 0;
        for (Thread thread : threads) {
            if (thread.getName().startsWith(prefix) && thread.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
