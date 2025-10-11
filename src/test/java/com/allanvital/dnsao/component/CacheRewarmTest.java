package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.RewarmCacheManager;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.exc.ConfException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.notification.EventType.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheRewarmTest extends TestHolder {

    private CacheManager cacheManager;
    private RewarmCacheManager rewarmCacheManager;
    private ResolverFactory resolverFactory;
    private String domain = "domain.com";
    private String ip = "10.10.10.10";
    private Message response;

    @BeforeEach
    public void setup() throws ConfException {
        this.loadConf("1udp-upstream-cache-rewarm.yml", false);
        super.startFakeDnsServer();
        resolverFactory = new ResolverFactory(null, conf.getResolver().getUpstreams());
        this.rewarmCacheManager = new RewarmCacheManager(conf.getCache(), resolverFactory, 0);
        this.cacheManager = new CacheManager(conf.getCache(), this.rewarmCacheManager);
        response = super.prepareSimpleMockResponse(domain, ip, 1);
    }

    @Test
    public void shouldRewarmThreeTimesBeforeDiscardingEntry() throws InterruptedException {
        cacheManager.put("A:" + domain, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 2);
        Assertions.assertEquals(2, fakeDnsServer.getCallCount());
        eventListener.waitEventCount(CACHE_REWARM_EXPIRED, 1);
        Assertions.assertTrue(rewarmCacheManager.isEmpty(), rewarmCacheManager.toString());
    }

    @Test
    public void shouldPromoteFromRewarmAndThenRewarmAgainLater() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 1);
        cacheManager.get(key);
        eventListener.waitEventCount(CACHE_REWARM_PROMOTION, 1);
        eventListener.waitEventCount(CACHE_REWARM, 3);
        Assertions.assertEquals(3, fakeDnsServer.getCallCount());
        eventListener.waitEventCount(CACHE_REWARM_EXPIRED, 1);
        Assertions.assertTrue(rewarmCacheManager.isEmpty(), rewarmCacheManager.toString());
    }

    @Test
    public void shouldPromoteAndResetTtlWhenRewarming() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 1);
        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 1);
        eventListener.assertCount(CACHE_REWARM_PROMOTION, 1);
        Assertions.assertEquals(1, fakeDnsServer.getCallCount());
    }

    @Test
    public void shouldRemoveLeastUsedFromMainCacheButPromoteCorrectlyWhenRequested() throws InterruptedException {
        String domain1 = "domain1.com";
        String ip1 = "10.10.10.50";
        String key1 = "A:" + domain1;

        String domain2 = "domain2.com";
        String ip2 = "10.10.10.60";
        String key2 = "A:" + domain2;

        String domain3 = "domain3.com";
        String ip3 = "10.10.10.70";
        String key3 = "A:" + domain3;

        Message response1 = super.prepareSimpleMockResponse(domain1, ip1, 1);
        Message response2 = super.prepareSimpleMockResponse(domain2, ip2, 1);
        Message response3 = super.prepareSimpleMockResponse(domain3, ip3, 1);

        cacheManager.put(key1, response1, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 1);
        cacheManager.put(key2, response2, 1L);
        cacheManager.put(key3, response3, 1L); //should remove the first on from cache

        cacheManager.get(key1);
        eventListener.assertCount(CACHE_HIT, 1);
        eventListener.assertCount(CACHE_REWARM_PROMOTION, 1);
    }

    @AfterEach
    public void tearDown() {
        this.rewarmCacheManager.shutdown();
        super.stopFakeDnsServer();
        eventListener.reset();
    }

}