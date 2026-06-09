package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheExpirationScenariosTest extends TestHolder {

    private CacheStats stats;

    public void setup(String config) throws Exception {
        safeStart(config);
        CacheManager cacheManager = assembler.getCacheManager();
        stats = cacheManager.getCacheStats();
        prepareSimpleMockResponse("domain.com", "10.10.10.10", 1);
        eventListener.reset();
    }

    @Test
    public void nonExpiredEntriesSurviveScavengerRun() throws Exception {
        setup("1udp-upstream-cache-removal.yml");
        executeRequestOnOwnServer("domain.com");

        eventListener.assertCount(CACHE_ADDED, 1, false);
        assertEquals(1, stats.getCurrentSize());
        eventListener.assertCount(SCAVENGER_RAN, 2, false);
        assertEquals(1, stats.getCurrentSize());
    }

    @Test
    public void serveExpiredEntrySurvivesWithinWindow() throws Exception {
        setup("1udp-upstream-cache-removal-stale.yml");

        executeRequestOnOwnServer("domain.com");
        eventListener.assertCount(CACHE_ADDED, 1, false);
        assertEquals(1, stats.getCurrentSize());

        testTimeProvider.walkNow(1500);

        eventListener.assertCount(SCAVENGER_RAN, 2, false);
        assertEquals(1, stats.getCurrentSize());
    }

    @Test
    public void serveExpiredEntryRemovedAfterWindow() throws Exception {
        setup("1udp-upstream-cache-removal-stale.yml");

        executeRequestOnOwnServer("domain.com");
        eventListener.assertCount(CACHE_ADDED, 1, false);
        assertEquals(1, stats.getCurrentSize());

        testTimeProvider.walkNow(2500);

        eventListener.assertCount(CACHE_REMOVED, 1, false);
        assertEquals(0, stats.getCurrentSize());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
