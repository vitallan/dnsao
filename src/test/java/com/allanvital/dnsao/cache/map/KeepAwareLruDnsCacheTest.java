package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.Constants;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.cache.SizeSnapshot;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.clock.RealTimeProvider;
import com.allanvital.dnsao.graph.TestTimeProvider;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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

    @Test
    void shouldRecordSnapshotOnConstruction() {
        AtomicLong nowRef = new AtomicLong(100_000L);
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null, 60_000L, 300_000L, nowRef::get);

        List<SizeSnapshot> history = cache.getSizeHistory();
        assertEquals(5, history.size());
        assertEquals(-180_000L, history.get(0).timestamp());
        assertEquals(60_000L, history.get(4).timestamp());
        assertEquals(0, history.get(4).size());
    }

    @Test
    void shouldAddSnapshotOnDemand() {
        AtomicLong nowRef = new AtomicLong(100_000L);
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null, 60_000L, 300_000L, nowRef::get);
        cache.put("k1", entry());
        cache.put("k2", entry());

        List<SizeSnapshot> history = cache.getSizeHistory();
        assertEquals(5, history.size());
        assertEquals(60_000L, history.get(4).timestamp());
        assertEquals(2, history.get(4).size());

        nowRef.addAndGet(60_000L);
        cache.put("k3", entry());
        history = cache.getSizeHistory();
        assertEquals(5, history.size());
        assertEquals(120_000L, history.get(4).timestamp());
        assertEquals(3, history.get(4).size());
    }

    @Test
    void shouldReflectCacheMutations() {
        AtomicLong nowRef = new AtomicLong(100_000L);
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null, 60_000L, 300_000L, nowRef::get);

        cache.put("a", entry());
        cache.put("b", entry());
        cache.put("c", entry());
        assertEquals(3, cache.getSizeHistory().get(4).size());

        cache.put("d", entry());
        cache.put("e", entry());
        nowRef.addAndGet(60_000L);
        assertEquals(5, cache.getSizeHistory().get(4).size());
        assertEquals(120_000L, cache.getSizeHistory().get(4).timestamp());
    }

    @Test
    void shouldAlignTimestampsToBucketBoundaries() {
        AtomicLong nowRef = new AtomicLong(100_123L);
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null, 60_000L, 300_000L, nowRef::get);

        assertEquals(60_000L, cache.getSizeHistory().get(4).timestamp());
    }

    @Test
    void shouldSlideWindowAsTimeAdvances() {
        AtomicLong nowRef = new AtomicLong(0L);
        KeepAwareLruDnsCache cache = new KeepAwareLruDnsCache(10, null, 30_000L, 90_000L, nowRef::get);
        cache.put("a", entry());

        List<SizeSnapshot> history = cache.getSizeHistory();
        assertEquals(3, history.size());
        assertEquals(0L, history.get(2).timestamp());
        assertEquals(1, history.get(2).size());

        nowRef.addAndGet(120_000L);
        cache.recordSizeSnapshot();
        history = cache.getSizeHistory();
        assertEquals(3, history.size());
        assertEquals(120_000L, history.get(2).timestamp());
        assertEquals(1, history.get(2).size());
        assertEquals(60_000L, history.get(0).timestamp());
    }

    private static DnsCacheEntry entry() {
        return new DnsCacheEntry(new Message(), 300L);
    }

}
