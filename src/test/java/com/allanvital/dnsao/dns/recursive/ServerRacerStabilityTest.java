package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServerRacerStabilityTest {

    @Test
    public void doesNotStartQueriesAgainstEveryAuthorityWhenWinnerArrivesQuickly() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch releaseBlockers = new CountDownLatch(1);
        TestResolverFactory resolverFactory = new TestResolverFactory(10, 75, releaseBlockers);
        DnssecDowngradeHandler dnssecHandler = new DnssecDowngradeHandler(DNSSecMode.OFF);
        ServerRacer serverRacer = new ServerRacer(executor, 1, resolverFactory, dnssecHandler);

        try {
            Map.Entry<NameServerAddress, StepResponse> responseEntry = serverRacer.race(resolverFactory.getServers(), resolverFactory.buildRequest());

            assertNotNull(responseEntry);
            assertEquals(TestResolverFactory.FAST_SERVER_IP, responseEntry.getKey().ip());
            sleep(150);
            assertTrue(resolverFactory.getStartedCount() < resolverFactory.getServers().size(),
                    "Expected the racer to avoid starting queries against every authority once an early winner is available");
        } finally {
            releaseBlockers.countDown();
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    public void cancelsSlowLosersAfterWinnerReturns() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch releaseBlockers = new CountDownLatch(1);
        TestResolverFactory resolverFactory = new TestResolverFactory(3, 75, releaseBlockers);
        DnssecDowngradeHandler dnssecHandler = new DnssecDowngradeHandler(DNSSecMode.OFF);
        ServerRacer serverRacer = new ServerRacer(executor, 1, resolverFactory, dnssecHandler);

        try {
            Map.Entry<NameServerAddress, StepResponse> responseEntry = serverRacer.race(resolverFactory.getServers(), resolverFactory.buildRequest());

            assertNotNull(responseEntry);
            assertEquals(TestResolverFactory.FAST_SERVER_IP, responseEntry.getKey().ip());
            assertTrue(waitUntilActiveBlockers(resolverFactory, 0, 300),
                    "Expected slow loser tasks to be cancelled promptly after a winner is selected");
        } finally {
            releaseBlockers.countDown();
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    private boolean waitUntilActiveBlockers(TestResolverFactory resolverFactory, int expectedActiveCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (resolverFactory.getActiveBlockerCount() == expectedActiveCount) {
                return true;
            }
            Thread.sleep(10);
        }
        return resolverFactory.getActiveBlockerCount() == expectedActiveCount;
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class TestResolverFactory extends StepResolverFactory {

        private static final String FAST_SERVER_IP = "127.0.0.200";
        private static final String QUERY_NAME = "allanvital.com.";
        private static final String RESPONSE_IP = "10.0.0.25";

        private final int blockerCount;
        private final long fastDelayMs;
        private final CountDownLatch releaseBlockers;
        private final AtomicInteger startedCount = new AtomicInteger(0);
        private final AtomicInteger activeBlockerCount = new AtomicInteger(0);

        private TestResolverFactory(int blockerCount, long fastDelayMs, CountDownLatch releaseBlockers) {
            super(1000);
            this.blockerCount = blockerCount;
            this.fastDelayMs = fastDelayMs;
            this.releaseBlockers = releaseBlockers;
        }

        public List<NameServerAddress> getServers() {
            List<NameServerAddress> servers = new ArrayList<>();
            servers.add(new NameServerAddress(FAST_SERVER_IP, 53));
            for (int i = 0; i < blockerCount; i++) {
                servers.add(new NameServerAddress("127.0.0." + (50 + i), 53));
            }
            return List.copyOf(servers);
        }

        public StepRequest buildRequest() throws Exception {
            return new StepRequest(Name.fromString(QUERY_NAME), Type.A, DClass.IN, DNSSecMode.OFF);
        }

        public int getStartedCount() {
            return startedCount.get();
        }

        public int getActiveBlockerCount() {
            return activeBlockerCount.get();
        }

        @Override
        public StepResolver create(String ip, int port) {
            if (FAST_SERVER_IP.equals(ip)) {
                return request -> {
                    startedCount.incrementAndGet();
                    sleep(fastDelayMs);
                    return new StepResponse(MessageHelper.buildAResponse(request.toWireMessage(), RESPONSE_IP, 300));
                };
            }
            return request -> {
                startedCount.incrementAndGet();
                activeBlockerCount.incrementAndGet();
                try {
                    releaseBlockers.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    activeBlockerCount.decrementAndGet();
                }
                return null;
            };
        }

        private void sleep(long delayMs) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
