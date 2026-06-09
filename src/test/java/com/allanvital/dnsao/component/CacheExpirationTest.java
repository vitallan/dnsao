package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheExpirationTest extends TestHolder {

    private CacheManager cacheManager;
    private String domain = "domain.com";
    private String ip = "10.10.10.10";
    private CacheStats stats;

    @BeforeEach
    public void setup() throws ConfException {
        safeStart("1udp-upstream-cache-removal.yml");
        cacheManager = assembler.getCacheManager();
        stats = cacheManager.getCacheStats();
        super.prepareSimpleMockResponse(domain, ip, 1);
    }

    @Test
    public void shouldReduceCacheSizeAfterEntryExpires() throws Exception {
        executeRequestOnOwnServer(domain);
        eventListener.assertCount(EventType.CACHE_ADDED, 1, false);
        assertEquals(1, stats.getCurrentSize());
        
        testTimeProvider.walkNow(1500);

        waitUntilCacheReachesSize(0);

        assertEquals(0, stats.getCurrentSize());
    }

    private void waitUntilCacheReachesSize(int expectedSize) throws Exception {
        long timeoutMs = 5000L;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (stats.getCurrentSize() == expectedSize) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Expected cacheSize never reached " + expectedSize + ", size=" + stats.getCurrentSize());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
