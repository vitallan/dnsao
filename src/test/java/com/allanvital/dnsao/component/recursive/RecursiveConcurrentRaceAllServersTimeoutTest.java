package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
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

public class RecursiveConcurrentRaceAllServersTimeoutTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FIRST_SILENT_IP = "127.0.0.104";
    private static final String SECOND_SILENT_IP = "127.0.0.105";

    private FakeServer firstSilentServer;
    private FakeServer secondSilentServer;

    @Override
    protected String recursiveConfigResource() {
        return "recursive-mode-stub-timeout1.yml";
    }

    @Override
    protected void beforeServerStart() throws Exception {
        firstSilentServer = startSilentFakeUdpServer();
        trackExtraFakeServer(firstSilentServer);

        secondSilentServer = startSilentFakeUdpServer();
        trackExtraFakeServer(secondSilentServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(FIRST_SILENT_IP, firstSilentServer.getPort()),
                new NameServerAddress(SECOND_SILENT_IP, secondSilentServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_SILENT_IP, firstSilentServer.getPort());
        stepResolverFactory.setRoute(SECOND_SILENT_IP, secondSilentServer.getPort());
    }

    @Test
    public void allServersTimeoutReturnsServfail() throws IOException {
        Instant start = Instant.now();
        Message response = executeRequestOnOwnServer(DOMAIN);
        Instant end = Instant.now();

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        long elapsedMs = Duration.between(start, end).toMillis();
        assertTrue(elapsedMs < 2000,
                "Total timeout should be ~1x per-server timeout (1s), not 2x. Elapsed: " + elapsedMs + "ms");
    }
}
