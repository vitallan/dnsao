package com.allanvital.dnsao.component;

import com.allanvital.dnsao.SystemGraph;
import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.RewarmScheduler;
import com.allanvital.dnsao.cache.rewarm.RewarmWorker;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.utils.ThreadShop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.notification.EventType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheRewarmTest extends TestHolder {

    private CacheManager cacheManager;
    private ResolverFactory resolverFactory;
    private String domain = "domain.com";
    private String ip = "10.10.10.10";
    private Message response;
    private RewarmWorker rewarmWorker;
    private ExecutorService executorService;
    private RewarmScheduler rewarmScheduler;

    @BeforeEach
    public void setup() throws ConfException {
        this.loadConf("1udp-upstream-cache-rewarm.yml", false);
        super.startFakeDnsServer();
        resolverFactory = new ResolverFactory(null, conf.getResolver().getUpstreams());
        rewarmScheduler = new RewarmScheduler(500);
        this.cacheManager = new CacheManager(conf.getCache(), rewarmScheduler);
        executorService = ThreadShop.buildExecutor("test-rewarm", 1);
        rewarmWorker = SystemGraph.scheduleRewarmWorker(executorService, conf.getCache(), rewarmScheduler, cacheManager, resolverFactory);
        response = super.prepareSimpleMockResponse(domain, ip, 1);
    }

    @Test
    public void shouldRewarmTwoTimesBeforeDiscardingEntry() throws InterruptedException {
        cacheManager.put("A:" + domain, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 2);
        assertEquals(2, fakeDnsServer.getCallCount());
        eventListener.waitEventCount(CACHE_REWARM_EXPIRED, 1);
    }

    @Test
    public void shouldPromoteFromRewarmAndThenRewarmAgainLater() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 1);
        cacheManager.get(key);
        eventListener.waitEventCount(CACHE_REWARM, 3);
        assertEquals(3, fakeDnsServer.getCallCount());
        eventListener.waitEventCount(CACHE_REWARM_EXPIRED, 1);
    }

    @Test
    public void shouldResetTtlAndRewarmCountWhenHittingRewarmed() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        eventListener.waitEventCount(CACHE_REWARM, 1);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 1);
        eventListener.waitEventCount(CACHE_REWARM, 2);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 2);
        eventListener.waitEventCount(CACHE_REWARM, 3);

        cacheManager.get(key);
        eventListener.assertCount(CACHE_HIT, 3);
        eventListener.waitEventCount(CACHE_REWARM, 4);

        assertEquals(4, fakeDnsServer.getCallCount());
    }

    @Test
    public void shouldNotFireRewarmAfterCancel() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        rewarmScheduler.cancel(key);
        Thread.sleep(2000);
        assertEquals(0, fakeDnsServer.getCallCount());
        eventListener.assertCount(CACHE_REWARM, 0);
    }

    @Test
    public void shouldNotReescheduleOnCacheHit() throws InterruptedException {
        String key = "A:" + domain;
        cacheManager.put(key, response, 1L);
        cacheManager.get(key);
        eventListener.assertCount(REWARM_TASK_SCHEDULED, 1);
        assertEquals(1, rewarmScheduler.queue().size());
    }

    @AfterEach
    public void tearDown() {
        rewarmWorker.shutdown();
        while (rewarmWorker.isRunning()) {
            Thread.yield();
        }
        executorService.shutdownNow();
        while (!executorService.isTerminated()) {
            Thread.yield();
        }
        super.stopFakeDnsServer();
        eventListener.reset();
    }

}