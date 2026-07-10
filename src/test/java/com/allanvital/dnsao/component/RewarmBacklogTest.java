package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_HIT;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RewarmBacklogTest extends TestHolder {

    private static final String DOMAIN_A = "rewarm-a.example.com";
    private static final String DOMAIN_B = "rewarm-b.example.com";
    private static final String DOMAIN_C = "rewarm-c.example.com";

    private CacheManager cacheManager;
    private SlowResolver slowResolver;

    @BeforeEach
    void setup() throws ConfException {
        slowResolver = new SlowResolver(1500);
        registerOverride(new FixedTimeRewarmScheduler(1000));
        registerOverride(new TestResolverProvider(List.of(slowResolver)));
        safeStart("1udp-upstream-cache-rewarm.yml");
        cacheManager = assembler.getCacheManager();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        safeStop();
    }

    @Test
    void laterEntriesShouldStillBeFreshEvenWhenAnEarlierRewarmIsSlow() throws Exception {
        cacheManager.put(cacheKey(DOMAIN_A), aResponse(DOMAIN_A, "10.0.0.1"), 2L);
        testTimeProvider.walkNow(10);
        cacheManager.put(cacheKey(DOMAIN_C), aResponse(DOMAIN_C, "10.0.0.3"), 2L);

        testTimeProvider.walkNow(1200);
        eventListener.assertCount(CACHE_REWARM, 2, false);
        testTimeProvider.walkNow(1200);

        Message response = executeRequestOnOwnServer(dnsServer, DOMAIN_C, false);
        assertEquals("10.10.10.10", MessageHelper.extractIpFromResponseMessage(response));
        eventListener.assertCount(CACHE_HIT, 1, false);
    }

    private Message aResponse(String domain, String ip) {
        return MessageHelper.buildAResponse(MessageHelper.buildARequest(domain), ip, 2);
    }

    private String cacheKey(String domain) {
        return "A:" + domain + ".";
    }
}
