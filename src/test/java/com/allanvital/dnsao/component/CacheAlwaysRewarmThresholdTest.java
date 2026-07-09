package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CacheAlwaysRewarmThresholdTest extends TestHolder {

    private static final String HOT = "hot-threshold.com";
    private static final String COLD = "cold-threshold.com";
    private static final String KEEP = "keep-threshold.com";

    private FixedTimeRewarmScheduler rewarmScheduler;
    private CacheManager cacheManager;

    @BeforeEach
    public void setup() throws ConfException {
        rewarmScheduler = new FixedTimeRewarmScheduler(500);
        registerOverride(rewarmScheduler);
    }

    @Test
    public void hottestNonKeepEntryShouldKeepRewarmingAfterMaxRewarmCount() throws Exception {
        safeStartThresholdScenario("cache/1udp-cache-rewarm-threshold-top1.yml");

        executeRequestOnOwnServer(HOT);
        executeRequestOnOwnServer(COLD);
        executeRequestOnOwnServer(HOT);

        assertTrue(cacheManager.shouldAlwaysRewarm(cacheKey(HOT), MessageHelper.buildARequest(HOT).getQuestion()));
        assertFalse(cacheManager.shouldAlwaysRewarm(cacheKey(COLD), MessageHelper.buildARequest(COLD).getQuestion()));
    }

    @Test
    public void keepEntriesShouldNotConsumeTheTopNonKeepThresholdBudget() throws Exception {
        safeStartThresholdScenario("cache/1udp-cache-rewarm-threshold-top1-with-keep.yml");

        executeRequestOnOwnServer(KEEP);
        executeRequestOnOwnServer(HOT);
        executeRequestOnOwnServer(COLD);
        executeRequestOnOwnServer(HOT);

        assertTrue(cacheManager.shouldAlwaysRewarm(cacheKey(KEEP), MessageHelper.buildARequest(KEEP).getQuestion()));
        assertTrue(cacheManager.shouldAlwaysRewarm(cacheKey(HOT), MessageHelper.buildARequest(HOT).getQuestion()));
        assertFalse(cacheManager.shouldAlwaysRewarm(cacheKey(COLD), MessageHelper.buildARequest(COLD).getQuestion()));
    }

    private void safeStartThresholdScenario(String config) throws Exception {
        loadConf(config);
        conf.getMisc().setQueryLog(false);
        startFakeServer();
        prepareSimpleMockResponse(KEEP, "10.10.10.10", 1);
        prepareSimpleMockResponse(HOT, "10.10.10.11", 1);
        prepareSimpleMockResponse(COLD, "10.10.10.12", 1);
        safeStartWithPresetConf(true);
        cacheManager = assembler.getCacheManager();
    }

    private String cacheKey(String domain) {
        return "A:" + domain + ".";
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }
}
