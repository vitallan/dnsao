package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.BlockingResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.REWARM_TASK_SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewarmCoordinatorSaturationTest extends TestHolder {

    private static final String DOMAIN_A = "sat-a.example.com";
    private static final String DOMAIN_B = "sat-b.example.com";

    private BlockingResolver blockingResolver;
    private CacheManager cacheManager;

    @BeforeEach
    void setup() throws ConfException {
        loadConf("1udp-upstream-cache-rewarm.yml");
        conf.getMisc().setQueryLog(false);
        conf.getCache().setRewarmWorkerPoolSize(1);
        blockingResolver = new BlockingResolver("10.10.10.10", 300);
        registerOverride(new FixedTimeRewarmScheduler(1000));
        registerOverride(new TestResolverProvider(List.of(blockingResolver)));
        safeStartWithPresetConf();
        cacheManager = assembler.getCacheManager();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        safeStop();
    }

    @Test
    void coordinatorShouldRequeueWhenAllWorkersAreBusy() throws Exception {
        cacheManager.put("A:" + DOMAIN_A, response(DOMAIN_A, "10.0.0.1"), 2L);
        testTimeProvider.walkNow(10);
        cacheManager.put("A:" + DOMAIN_B, response(DOMAIN_B, "10.0.0.2"), 2L);

        eventListener.assertCount(REWARM_TASK_SCHEDULED, 2, false);
        testTimeProvider.walkNow(1200);
        assertTrue(blockingResolver.started.await(1, TimeUnit.SECONDS), "first rewarm should occupy the only worker");

        eventListener.assertCount(REWARM_TASK_SCHEDULED, 3, false);
        blockingResolver.proceed.countDown();
    }

    private Message response(String domain, String ip) {
        return MessageHelper.buildAResponse(MessageHelper.buildARequest(domain), ip, 2);
    }
}
