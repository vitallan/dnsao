package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class UpstreamThreadPoolSaturationE2ETest extends TestHolder {

    private static final int CLIENT_COUNT = 20;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("saturation/upstream-pool-1-queue-1-multiplier-3.yml");

        UpstreamResolverBuilder builder = queryInfraAssembler.getResolverBuilder();
        builder.setResolvers(List.of(
                new SlowResolver(200),
                new SlowResolver(200),
                new SlowResolver(200)
        ));
    }

    @Test
    @Timeout(30)
    public void allConcurrentQueriesCompleteUnderUpstreamSaturation() throws Exception {
        ExecutorService client = Executors.newFixedThreadPool(CLIENT_COUNT);
        try {
            List<Future<Message>> futures = new ArrayList<>();
            for (int i = 0; i < CLIENT_COUNT; i++) {
                final int idx = i;
                futures.add(client.submit(() -> {
                    SimpleResolver resolver = buildResolverWithTimeout();
                    Message request = MessageHelper.buildARequest("example-" + idx + ".com");
                    return resolver.send(request);
                }));
            }

            for (Future<Message> future : futures) {
                Message response = future.get(10, TimeUnit.SECONDS);
                assertNotNull(response);
                String ip = MessageHelper.extractIpFromResponseMessage(response);
                assertEquals("10.10.10.10", ip);
            }
        } finally {
            client.shutdownNow();
            client.awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    private SimpleResolver buildResolverWithTimeout() throws Exception {
        SimpleResolver resolver = super.buildResolver(dnsServer.getUdpPort());
        resolver.setTimeout(Duration.ofSeconds(5));
        return resolver;
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
