package com.allanvital.dnsao.stats;

import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.web.StatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.TestHolder.t;
import static com.allanvital.dnsao.notification.QueryResolvedBy.UPSTREAM;
import static org.junit.jupiter.api.Assertions.*;

public class UpstreamHitCountTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    StatsCollector statsCollector;

    @BeforeEach
    public void setup() {
        // 60 min / 5 min window = 12 buckets
        this.statsCollector = new StatsCollector(5 * 60_000L, 60 * 60_000L, nowRef::get);
    }

    @Test
    public void testCountOnUpstreamSummarization() {
        long e1 = t("2025-10-02T10:03:12Z");
        long e2 = t("2025-10-02T10:04:59Z");

        long e3 = t("2025-10-02T10:08:12Z");
        long e4 = t("2025-10-02T10:09:19Z");
        long e5 = t("2025-10-02T10:09:52Z");

        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e1));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e2));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.2", e3));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e4));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.3", e5));

        NavigableMap<Long, Map<String, Long>> upstreamBucketsRaw = statsCollector.getUpstreamBucketsRaw();
        long expectedStart = StatsCollector.truncateToWindow(e1, 5 * 60_000L);
        Map<String, Long> upstreamHits = upstreamBucketsRaw.get(expectedStart);
        assertTrue(upstreamHits.containsKey("1.1.1.1"));
        assertFalse(upstreamHits.containsKey("1.1.1.2"));
        assertFalse(upstreamHits.containsKey("1.1.1.3"));
        assertEquals(2, upstreamHits.get("1.1.1.1"));

        expectedStart = StatsCollector.truncateToWindow(e3, 5 * 60_000L);
        upstreamHits = upstreamBucketsRaw.get(expectedStart);
        assertTrue(upstreamHits.containsKey("1.1.1.1"));
        assertTrue(upstreamHits.containsKey("1.1.1.2"));
        assertTrue(upstreamHits.containsKey("1.1.1.3"));
        assertEquals(1, upstreamHits.get("1.1.1.1"));
        assertEquals(1, upstreamHits.get("1.1.1.2"));
        assertEquals(1, upstreamHits.get("1.1.1.3"));
    }

    @Test
    public void testCountOnUpstreamSummarizationAnchored() {
        AtomicLong customRef = new AtomicLong(t("2025-10-02T10:30:00Z"));
        statsCollector = new StatsCollector(5 * 60_000L, 40 * 60_000L, customRef::get);

        long e1 = t("2025-10-02T10:03:12Z");
        long e2 = t("2025-10-02T10:04:12Z");
        long e3 = t("2025-10-02T10:13:59Z");
        long e4 = t("2025-10-02T10:23:12Z");

        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e1));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e2));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e3));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", e4));

        NavigableMap<Long, Map<String, Long>> upstreamBuckets = statsCollector.getUpstreamBucketsAnchoredToNow();

        assertEquals(8, upstreamBuckets.size(), upstreamBuckets.toString());

        long[] expectedStarts = {
                t("2025-10-02T09:55:00Z"),
                t("2025-10-02T10:00:00Z"),
                t("2025-10-02T10:05:00Z"),
                t("2025-10-02T10:10:00Z"),
                t("2025-10-02T10:15:00Z"),
                t("2025-10-02T10:20:00Z"),
                t("2025-10-02T10:25:00Z"),
                t("2025-10-02T10:30:00Z")
        };
        long[] expectedVals = {0, 2, 0, 1, 0, 1, 0, 0};

        for (int i = 0; i < expectedVals.length; i++) {
            Map<String, Long> upstreamBucketOnInterval = upstreamBuckets.get(expectedStarts[i]);
            assertNotNull(upstreamBucketOnInterval, "failed on index " + i);
            assertEquals(expectedVals[i], upstreamBucketOnInterval.get("1.1.1.1"), "failed on index " + i);
        }
    }

}
