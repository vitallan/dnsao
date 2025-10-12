package com.allanvital.dnsao.stats;

import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.StatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static com.allanvital.dnsao.TestHolder.t;
import static com.allanvital.dnsao.notification.QueryResolvedBy.CACHE;
import static com.allanvital.dnsao.notification.QueryResolvedBy.UPSTREAM;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MovingWindowStatsCollectionTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:10:00Z"));
    StatsCollector statsCollector;

    @BeforeEach
    public void setup() {
        // 60 min / 5 min window = 12 buckets
        this.statsCollector = new StatsCollector(5 * 60_000L, 60 * 60_000L, nowRef::get);
    }

    @Test
    public void aggregatesEventsInSameFiveMinuteBucketNow() {
        long e1 = t("2025-10-02T10:07:12Z");
        long e2 = t("2025-10-02T10:09:59Z");

        statsCollector.receiveNewQuery(new QueryEvent(e1));
        statsCollector.receiveNewQuery(new QueryEvent(e2));

        NavigableMap<Long, Long> raw = statsCollector.getCountsRaw();
        assertEquals(1, raw.size(), "there should be only one bucket");
        long bucketStart = raw.firstKey();
        assertEquals(2L, raw.get(bucketStart), "there should be count = 2 on available bucket");
        long expectedStart = StatsCollector.truncateToWindow(e1, 5 * 60_000L);
        assertEquals(expectedStart, bucketStart);
    }

    @Test
    public void trimHappensOnReadWhenNowMovesPastWindow() {
        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z")));

        assertFalse(statsCollector.getCountsRaw().isEmpty());

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later

        NavigableMap<Long, Long> rawAfter = statsCollector.getCountsRaw();
        assertTrue(rawAfter.isEmpty(), "should be trimmed out of the 60-min window");
        NavigableMap<Long, Long> counts = statsCollector.getCountsFilledAnchoredToNow();
        assertEquals(12, counts.size());
    }

    @Test
    public void filledAnchoredToNowReturnsContinuousSeriesWithZeros() {
        AtomicLong customRef = new AtomicLong(t("2025-10-02T10:25:00Z"));
        StatsCollector statsCollector = new StatsCollector(5 * 60_000L, 20 * 60_000L, customRef::get);

        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:10:01Z")));

        NavigableMap<Long, Long> filled = statsCollector.getCountsFilledAnchoredToNow();
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
        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z")));
        assertEquals(1, statsCollector.getQueryCount());

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(1, statsCollector.getQueryCount());

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(0, statsCollector.getQueryCount());
    }

    @Test
    public void shouldKeepCountOnlyOnCurrentWindowBySource() {
        statsCollector.receiveNewQuery(new QueryEvent(CACHE, null, t("2025-10-02T10:07:00Z")));
        assertEquals(1, statsCollector.getQueryCount(CACHE));

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(1, statsCollector.getQueryCount(CACHE));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(0, statsCollector.getQueryCount(CACHE));
    }

    @Test
    public void shouldKeepElapsedTimeOnlyOnCurrentWindow() {
        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:00Z"), 100));
        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:01Z"), 200));
        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:07:02Z"), 300));

        assertEquals(200, statsCollector.getQueryElapsedTime());

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to 30 minutes later
        assertEquals(200, statsCollector.getQueryElapsedTime());

        statsCollector.receiveNewQuery(new QueryEvent(t("2025-10-02T12:07:02Z"), 300));
        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        assertEquals(300, statsCollector.getQueryElapsedTime());
    }

    @Test
    public void shouldCountGeneralByUpstreamOnlyOnCurrentWindowByUpstream() {
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:07:00Z")));
        statsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:08:00Z")));

        ConcurrentHashMap<String, LongAdder> upstreamHits = statsCollector.getUpstreamIndividualHits();
        assertEquals(2, upstreamHits.get("1.1.1.1").sum());

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        upstreamHits = statsCollector.getUpstreamIndividualHits();
        assertEquals(0, upstreamHits.get("1.1.1.1").sum());
    }

    @Test
    public void getOrderedQueryEvents() {
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = new QueryEvent(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q4 = new QueryEvent(t("2025-10-02T09:51:00Z"), 100);

        statsCollector.receiveNewQuery(q1);
        statsCollector.receiveNewQuery(q2);
        statsCollector.receiveNewQuery(q3);
        statsCollector.receiveNewQuery(q4);

        List<QueryEvent> orderedQueryEvents = statsCollector.getOrderedQueryEvents();
        assertTrue(orderedQueryEvents.containsAll(List.of(q1, q2, q3, q4)));
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));
        assertEquals(q1, orderedQueryEvents.get(3));

        nowRef.set(t("2025-10-02T10:30:00Z")); // walk the "now" to half an hour later

        orderedQueryEvents = statsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q2), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q3), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q4), orderedQueryEvents.toString());
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));

        nowRef.set(t("2025-10-02T10:40:00Z")); // walk the "now" to half an hour later

        orderedQueryEvents = statsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q2), orderedQueryEvents.toString());
        assertFalse(orderedQueryEvents.contains(q3), orderedQueryEvents.toString());
        assertTrue(orderedQueryEvents.contains(q4), orderedQueryEvents.toString());
        assertEquals(q4, orderedQueryEvents.get(0));

        nowRef.set(t("2025-10-02T12:10:00Z")); // walk the "now" to two hours later
        orderedQueryEvents = statsCollector.getOrderedQueryEvents();
        assertFalse(orderedQueryEvents.contains(q1));
        assertFalse(orderedQueryEvents.contains(q2));
        assertFalse(orderedQueryEvents.contains(q3));
        assertFalse(orderedQueryEvents.contains(q4));
    }

}