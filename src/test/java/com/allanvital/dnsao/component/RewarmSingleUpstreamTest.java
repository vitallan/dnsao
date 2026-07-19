package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RewarmSingleUpstreamTest extends TestHolder {

    private static final String DOMAIN = "single-rewarm.example.com";

    private CacheManager cacheManager;
    private CountingResolver firstResolver;
    private CountingResolver secondResolver;

    @BeforeEach
    public void setup() throws ConfException {
        firstResolver = new CountingResolver("first", "10.0.0.1");
        secondResolver = new CountingResolver("second", "10.0.0.2");
        registerOverride(new FixedTimeRewarmScheduler(1000));
        registerOverride(new TestResolverProvider(List.of(firstResolver, secondResolver)));
        safeStart("1-udp-upstream-cache-multiplier.yml");
        cacheManager = assembler.getCacheManager();
    }

    @Test
    public void rewarmShouldSendOnlyOneQueryUpstreamEvenWhenMultiplierIsGreaterThanOne() throws Exception {
        cacheManager.put("A:" + DOMAIN + ".", MessageHelper.buildAResponse(MessageHelper.buildARequest(DOMAIN), "10.0.0.10", 2), 2L);

        testTimeProvider.walkNow(1200, TimeUnit.MILLISECONDS);
        eventListener.assertCount(CACHE_REWARM, 1, false);

        assertEquals(1, firstResolver.getCount() + secondResolver.getCount());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    private static class CountingResolver implements UpstreamResolver {
        private final String name;
        private final String ip;
        private final AtomicInteger count = new AtomicInteger();

        private CountingResolver(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        @Override
        public String getIp() {
            return name;
        }

        @Override
        public int getPort() {
            return 53;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Message send(Message query) throws IOException {
            count.incrementAndGet();
            return MessageHelper.buildAResponse(query, ip, 2);
        }

        int getCount() {
            return count.get();
        }
    }
}
