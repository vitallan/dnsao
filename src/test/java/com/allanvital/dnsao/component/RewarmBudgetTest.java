package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;

public class RewarmBudgetTest extends TestHolder {

    private CacheManager cacheManager;

    @BeforeEach
    public void setup() throws ConfException {
        loadConf("1udp-upstream-cache-rewarm.yml");
        conf.getMisc().setQueryLog(false);
        conf.getCache().setMaxRewarmPerMinute(60);
        conf.getCache().setRewarmWorkerPoolSize(3);
        registerOverride(new FixedTimeRewarmScheduler(1000));
        safeStartWithPresetConf();
        cacheManager = assembler.getCacheManager();
    }

    @Test
    public void rewarmBudgetShouldBeSpreadAcrossSeconds() throws Exception {
        cacheManager.put("A:budget-a.example.com.", response("budget-a.example.com"), 2L);
        cacheManager.put("A:budget-b.example.com.", response("budget-b.example.com"), 2L);

        testTimeProvider.walkNow(1200L);
        eventListener.assertCount(CACHE_REWARM, 1, false);

        testTimeProvider.walkNow(1000L);
        eventListener.assertCount(CACHE_REWARM, 2, false);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    private Message response(String domain) {
        Message request = MessageHelper.buildARequest(domain);
        Message response = MessageHelper.buildAResponse(request, "10.10.10.10", 2);
        fakeUpstreamServer.mockResponse(request, response);
        return response;
    }
}
