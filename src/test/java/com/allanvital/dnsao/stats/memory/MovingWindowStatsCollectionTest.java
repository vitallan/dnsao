package com.allanvital.dnsao.stats.memory;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.memory.MemoryStatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.CACHE;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MovingWindowStatsCollectionTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:10:00Z"));
    MemoryStatsCollector memoryStatsCollector;

    @BeforeEach
    public void setup() {
        // 60 min / 5 min window = 12 buckets
        this.memoryStatsCollector = new MemoryStatsCollector(5 * 60_000L, 60 * 60_000L, nowRef::get);
    }

    @Test
    public void aggregatesEventsInSameFiveMinuteBucketNow() {
        long e1 = t("2025-10-02T10:07:12Z");
        long e2 = t("2025-10-02T10:09:59Z");

        memoryStatsCollector.receiveNewQuery(new QueryEvent(e1));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(e2));

        NavigableMap<Long, Long> raw = memoryStatsCollector.getCountsRaw();
        assertEquals(1, raw.size(), "there should be only one bucket");
        long bucketStart = raw.firstKey();
        assertEquals(2L, raw.get(bucketStart), "there should be count = 2 on available bucket");
        long expectedStart = MemoryStatsCollector.truncateToWindow(e1, 5 * 60_000L);
        assertEquals(expectedStart, bucketStart);
    }

    @Test
    public void trimHappensOnReadWhenNowMovesPastWindow() {
        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z")));

        assertFalse(memoryStatsCollector.getCountsRaw().isEmpty());

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later

        NavigableMap<Long, Long> rawAfter = memoryStatsCollector.getCountsRaw();
        assertTrue(rawAfter.isEmpty(), "should be trimmed out of the 60-min window");
        NavigableMap<Long, Long> counts = memoryStatsCollector.getCountsFilledAnchoredToNow();
        assertEquals(12, counts.size());
    }

    @Test
    public void filledAnchoredToNowReturnsContinuousSeriesWithZeros() {
        AtomicLong customRef = new AtomicLong(t("2025-10-02T10:25:00Z"));
        MemoryStatsCollector memoryStatsCollector = new MemoryStatsCollector(5 * 60_000L, 20 * 60_000L, customRef::get);

        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:10:01Z")));

        NavigableMap<Long, Long> filled = memoryStatsCollector.getCountsFilledAnchoredToNow();
        assertEquals(4, filled.size());

        long[] expectedStarts = {
                t("2025-10-02T10:10:00Z"),
                t("2025-10-02T10:15:00Z"),
                t("2025-10-02T10:20:00Z"),
                t("2025-10-02T10:25:00Z")
        };
        long[] expectedVals = {1, 0, 0, 0};

        int i = 0;
        Set<Map.Entry<Long, Long>> entries = filled.entrySet();
        for (Map.Entry<Long, Long> entry : entries) {
            assertEquals(expectedStarts[i], entry.getKey(), "unexpected bucket idx=" + i);
            assertEquals(expectedVals[i], entry.getValue(), "unexpected value idx=" + i);
            i++;
        }
    }

    @Test
    public void shouldKeepCountOnlyOnCurrentWindow() {
        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z")));
        assertEquals(1, memoryStatsCollector.getQueryCount(null));

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(1, memoryStatsCollector.getQueryCount(null));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(0, memoryStatsCollector.getQueryCount(null));
    }

    @Test
    public void shouldKeepCountOnlyOnCurrentWindowBySource() {
        memoryStatsCollector.receiveNewQuery(new QueryEvent(CACHE, null, t("2025-10-02T10:07:00Z")));
        assertEquals(1, memoryStatsCollector.getQueryCount(CACHE));

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(1, memoryStatsCollector.getQueryCount(CACHE));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(0, memoryStatsCollector.getQueryCount(CACHE));
    }

    @Test
    public void shouldKeepElapsedTimeOnlyOnCurrentWindow() {
        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z"), 100));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:01Z"), 200));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:02Z"), 300));

        assertEquals(200, memoryStatsCollector.getQueryElapsedTime());

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(200, memoryStatsCollector.getQueryElapsedTime());

        memoryStatsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T12:07:02Z"), 300));
        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(300, memoryStatsCollector.getQueryElapsedTime());
    }

    @Test
    public void shouldCountGeneralByUpstreamOnlyOnCurrentWindowByUpstream() {
        memoryStatsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:07:00Z")));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:08:00Z")));

        Map<String, Long> upstreamHits = memoryStatsCollector.getUpstreamIndividualHits();
        assertEquals(2, upstreamHits.get("1.1.1.1"));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        upstreamHits = memoryStatsCollector.getUpstreamIndividualHits();
        assertEquals(0, upstreamHits.get("1.1.1.1"));
    }

    @Test
    public void getOrderedQueryEvents() {
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        q1.setDomain("test.com.");
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:32:00Z"), 100);
        q2.setDomain("test.com.");
        QueryEvent q3 = new QueryEvent(t("2025-10-02T09:41:00Z"), 100);
        q3.setDomain("test.com.");
        QueryEvent q4 = new QueryEvent(t("2025-10-02T09:51:00Z"), 100);
        q4.setDomain("test.com.");

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents();
        assertTrue(orderedQueryEvents.containsAll(List.of(q1, q2, q3, q4)));
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));
        assertEquals(q1, orderedQueryEvents.get(3));

        nowRef.set(t("2025-10-02T10:30:00Z")); // walk the "now" to half an hour later

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q2), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q3), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q4), orderedQueryEvents.toString());
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to half an hour later

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q2), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q3), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q4), orderedQueryEvents.toString());
        assertEquals(q4, orderedQueryEvents.get(0));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1));
        assertFalse(orderedQueryEvents.contains(q2));
        assertFalse(orderedQueryEvents.contains(q3));
        assertFalse(orderedQueryEvents.contains(q4));
    }

}