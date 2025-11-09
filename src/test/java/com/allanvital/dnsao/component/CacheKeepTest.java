package com.allanvital.dnsao.component;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheKeepTest extends TestHolder {

    private String KEEP1 = "url-to-keep1.com";
    private String KEEP2 = "url-to-keep2.com";
    private String NO_KEEP = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        loadConf("1udp-cache-keep.yml");
        conf.getMisc().setQueryLog(false);
        startFakeServer();
        prepareSimpleMockResponse(KEEP1, "10.10.10.10", 5);
        prepareSimpleMockResponse(KEEP2, "10.10.10.10", 5);
        prepareSimpleMockResponse(NO_KEEP, "10.10.10.10", 5);
        safeStartWithPresetConf(true);
    }

    @Test
    public void cacheKeepEntriesShouldStartAlreadyPrecached() throws Exception {
        eventListener.assertCount(CACHE_ADDED, 2, false);
        executeRequestOnOwnServer(KEEP1);
        executeRequestOnOwnServer(KEEP2);
        executeRequestOnOwnServer(NO_KEEP);
        eventListener.assertCount(CACHE_HIT, 2, false);
        eventListener.assertCount(CACHE_ADDED, 3, false);
    }

    @Test
    public void cacheKeepEntriesShouldBeKeptOnCacheEvenAfterRewarmTimesAreReached() throws InterruptedException, IOException {
        executeRequestOnOwnServer(KEEP1);
        executeRequestOnOwnServer(KEEP2);
        eventListener.assertCount(CACHE_HIT, 2, false);
        testTimeProvider.walkNow(6000);
        eventListener.assertCount(CACHE_REWARM, 2, false);
        testTimeProvider.walkNow(6000);
        eventListener.assertCount(CACHE_REWARM, 4, false);
        testTimeProvider.walkNow(6000);
        eventListener.assertCount(CACHE_REWARM, 6, false);
        testTimeProvider.walkNow(6000);
        eventListener.assertCount(CACHE_REWARM, 8, false);
        testTimeProvider.walkNow(6000);
        eventListener.assertCount(CACHE_REWARM, 10, false);
        executeRequestOnOwnServer(KEEP1);
        executeRequestOnOwnServer(KEEP2);
        eventListener.assertCount(CACHE_HIT, 4, false);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
