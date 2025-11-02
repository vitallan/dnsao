package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.infra.notification.EventType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheRewarmTest extends TestHolder {

    private CacheManager cacheManager;
    private String domain = "domain.com";
    private String ip = "10.10.10.10";
    private Message response;
    private FixedTimeRewarmScheduler rewarmScheduler;

    @BeforeEach
    public void setup() throws ConfException {
        rewarmScheduler = new FixedTimeRewarmScheduler(500);
        registerOverride(rewarmScheduler);
        safeStart("1udp-upstream-cache-rewarm.yml");
        cacheManager = assembler.getCacheManager();
        response = super.prepareSimpleMockResponse(domain, ip, 1);
    }

    @Test
    public void shouldRewarmTwoTimesBeforeDiscardingEntry() throws InterruptedException {
        cacheManager.put("A:" + domain, response, 1L);

        eventListener.assertCount(CACHE_REWARM, 1);

        eventListener.assertCount(CACHE_REWARM, 2);

        assertEquals(2, fakeDnsServer.getCallCount());
        eventListener.assertCount(QUERY_RESOLVED, 2);
    }

    @Test
    public void shouldPromoteFromRewarmAndThenRewarmAgainLater() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);

        eventListener.assertCount(CACHE_REWARM, 1);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_REWARM, 3);

        assertEquals(3, fakeDnsServer.getCallCount());
        eventListener.assertCount(CACHE_REWARM_EXPIRED, 1);
        eventListener.assertCount(QUERY_RESOLVED, 3);
    }

    @Test
    public void shouldResetTtlAndRewarmCountWhenHittingRewarmed() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        eventListener.assertCount(CACHE_REWARM, 1);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 1);
        eventListener.assertCount(CACHE_REWARM, 2);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 2);
        eventListener.assertCount(CACHE_REWARM, 3);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 3);
        eventListener.assertCount(CACHE_REWARM, 4);

        assertEquals(4, fakeDnsServer.getCallCount());
        eventListener.assertCount(QUERY_RESOLVED, 4);
    }

    @Test
    public void shouldNotFireRewarmAfterCancel() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        rewarmScheduler.cancel(key);
        assertEquals(0, fakeDnsServer.getCallCount());
        eventListener.assertCount(CACHE_REWARM, 0);
        eventListener.assertCount(QUERY_RESOLVED, 0);
    }

    @Test
    public void shouldNotReescheduleOnCacheHit() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        cacheManager.get(key);
        eventListener.assertCount(REWARM_TASK_SCHEDULED, 1);
        assertEquals(1, rewarmScheduler.queue().size());
        eventListener.assertCount(QUERY_RESOLVED, 0);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}