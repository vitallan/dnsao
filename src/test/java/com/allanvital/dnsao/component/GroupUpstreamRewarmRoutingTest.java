package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamRoutingPolicy;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit.key;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GroupUpstreamRewarmRoutingTest extends TestHolder {

    private static final String DOMAIN = "rewarm-routing.example.com";
    private static final String DEFAULT_INITIAL_IP = "10.10.10.10";
    private static final String DEFAULT_REWARM_IP = "10.10.10.11";
    private static final String KIDS_INITIAL_IP = "10.10.10.30";
    private static final String KIDS_REWARM_IP = "10.10.10.31";

    private QueryProcessor processor;
    private CacheManager cacheManager;

    @BeforeEach
    public void setup() throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        registerOverride(new RoutingAwareResolverProvider());
        safeStart("groups/group-upstreams-rewarm.yml");
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
        cacheManager = assembler.getCacheManager();
    }

    @Test
    public void rewarmUsesTheOriginalQueryGroupUpstreamPool() throws Exception {
        Message request = MessageHelper.buildARequest(DOMAIN);
        DnsQuery initialResponse = processor.processExternalQuery(InetAddress.getByName("127.0.0.10"), request.toWire());
        assertEquals(KIDS_INITIAL_IP, MessageHelper.extractIpFromResponseMessage(initialResponse.getResponse()));

        testTimeProvider.walkNow(1200L);
        eventListener.assertCount(CACHE_REWARM, 1, false);

        DnsCacheEntry entry = cacheManager.safeGet(key(request));
        assertNotNull(entry);
        assertEquals(KIDS_REWARM_IP, MessageHelper.extractIpFromResponseMessage(entry.getResponse()));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    private static class RoutingAwareResolverProvider implements ResolverProvider {
        private final UpstreamResolver defaultResolver = new SequencedResolver("default", DEFAULT_INITIAL_IP, DEFAULT_REWARM_IP);
        private final UpstreamResolver kidsResolver = new SequencedResolver("kids", KIDS_INITIAL_IP, KIDS_REWARM_IP);

        @Override
        public List<UpstreamResolver> getAllResolvers() {
            return List.of(defaultResolver, kidsResolver);
        }

        @Override
        public List<UpstreamResolver> getResolversToUse() {
            return List.of(defaultResolver);
        }

        @Override
        public List<UpstreamResolver> getResolversToUse(UpstreamRoutingPolicy routingPolicy) {
            if (routingPolicy != null && "kids".equals(routingPolicy.group())) {
                return List.of(kidsResolver);
            }
            return List.of(defaultResolver);
        }
    }

    private static class SequencedResolver implements UpstreamResolver {
        private final String name;
        private final String initialIp;
        private final String rewarmIp;
        private final AtomicInteger calls = new AtomicInteger();

        private SequencedResolver(String name, String initialIp, String rewarmIp) {
            this.name = name;
            this.initialIp = initialIp;
            this.rewarmIp = rewarmIp;
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
            int call = calls.incrementAndGet();
            String ip = call == 1 ? initialIp : rewarmIp;
            return MessageHelper.buildAResponse(query, ip, 1);
        }
    }
}
