package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.Constants;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.clock.RealTimeProvider;
import com.allanvital.dnsao.graph.TestTimeProvider;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import static org.junit.jupiter.api.Assertions.*;

class KeepAwareLruDnsCacheTest {

    @Test
    void shouldReportCorrectSizeAndMaxSize() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(3, null);
        assertEquals(3, cache.getMaxSize());
        assertEquals(0, cache.getCurrentSize());

        cache.put("key1", entry());
        cache.put("key2", entry());
        cache.put("key3", entry());

        assertEquals(3, cache.getCurrentSize());
        assertEquals(3, cache.getMaxSize());
    }

    @Test
    void shouldIncrementEvictionCountOnLRUEviction() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(3, null);

        cache.put("key1", entry());
        cache.put("key2", entry());
        cache.put("key3", entry());

        assertEquals(0, cache.getEvictionCount());

        cache.put("key4", entry());

        assertEquals(3, cache.getCurrentSize());
        assertEquals(1, cache.getEvictionCount());

        cache.put("key5", entry());
        assertEquals(3, cache.getCurrentSize());
        assertEquals(2, cache.getEvictionCount());
    }

    @Test
    void shouldEvictLruEntryFirst() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(2, null);

        cache.put("a", entry());
        cache.put("b", entry());
        cache.put("c", entry());

        assertEquals(2, cache.getCurrentSize());
        assertNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("c"));
    }

    @Test
    void shouldNotEvictWhenUnderCapacity() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null);

        for (int i = 0; i < 5; i++) {
            cache.put("key" + i, entry());
        }

        assertEquals(5, cache.getCurrentSize());
        assertEquals(0, cache.getEvictionCount());
    }

    @Test
    void shouldWorkWithNullKeepProvider() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(100, null);

        for (int i = 0; i < 50; i++) {
            cache.put("key" + i, entry());
        }

        assertEquals(50, cache.getCurrentSize());
        assertEquals(100, cache.getMaxSize());
        assertEquals(0, cache.getEvictionCount());
    }

    @Test
    void shouldImplementCacheStatsInterface() {
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(5, null);
        assertInstanceOf(CacheStats.class, cache);
    }

    @Test
    void shouldNotCountEvictionsOlderThanWindow() {
        TestTimeProvider testTimeProvider = TestTimeProvider.getInstance();
        testTimeProvider.setNow(100_000L);
        Clock.setNewTimeProvider(testTimeProvider);
        try {
            KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(3, null);

            cache.put("key1", entry());
            cache.put("key2", entry());
            cache.put("key3", entry());

            cache.put("key4", entry());
            assertEquals(1, cache.getEvictionCount());

            testTimeProvider.walkNow(Constants.STATS_WINDOW_MS + 1_000L);

            assertEquals(0, cache.getEvictionCount());
        } finally {
            Clock.setNewTimeProvider(new RealTimeProvider());
        }
    }

    private static DnsCacheEntry entry() {
        return new DnsCacheEntry(new Message(), 300L);
    }

}
