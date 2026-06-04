package com.allanvital.dnsao.stats.memory;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.memory.MemoryStatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MovingWindowWithPagingStatsCollectionTest {

    private static QueryEvent eventWithDomain(long time, long elapsedTime) {
        QueryEvent event = new QueryEvent(time, elapsedTime);
        event.setDomain("test.com.");
        return event;
    }

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:10:00Z"));
    int pageSize = 3;
    MemoryStatsCollector memoryStatsCollector;

    @BeforeEach
    public void setup() {
        // 60 min / 5 min window = 12 buckets
        this.memoryStatsCollector = new MemoryStatsCollector(5 * 60_000L, 60 * 60_000L, pageSize, nowRef::get);
    }

    @Test
    public void getOrderedQueryEventsWithPages() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertFalse(orderedQueryEvents.contains(q1));
        assertTrue(orderedQueryEvents.containsAll(List.of(q4, q3, q2)));
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertEquals(1, orderedQueryEvents.size());
        assertTrue(orderedQueryEvents.contains(q1));
        assertEquals(q1, orderedQueryEvents.get(0));
    }

    @Test
    public void getOrderedQueryEventsEmptyCollectorReturnsEmpty() {
        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsPageBeyondAvailableReturnsEmpty() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(2, orderedQueryEvents.size());
        assertEquals(q2, orderedQueryEvents.get(0));
        assertEquals(q1, orderedQueryEvents.get(1));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(10);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsExactlyMultipleOfPageSize() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:33:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:34:00Z"), 100);
        QueryEvent q5 = eventWithDomain(t("2025-10-02T09:35:00Z"), 100);
        QueryEvent q6 = eventWithDomain(t("2025-10-02T09:36:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);
        memoryStatsCollector.receiveNewQuery(q5);
        memoryStatsCollector.receiveNewQuery(q6);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q6, orderedQueryEvents.get(0));
        assertEquals(q5, orderedQueryEvents.get(1));
        assertEquals(q4, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q3, orderedQueryEvents.get(0));
        assertEquals(q2, orderedQueryEvents.get(1));
        assertEquals(q1, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(2);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsOrdersCorrectlyWithinSameBucket() {
        // All events fall in the same 5-min bucket (09:30..09:34)
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:34:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:33:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q2, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q1, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertEquals(1, orderedQueryEvents.size());
        assertEquals(q4, orderedQueryEvents.get(0));
    }

    @Test
    public void getOrderedQueryEventsOrdersCorrectlyAcrossBucketBoundaries() {
        // Spans several 5-min buckets: 09:31/09:32, 09:36/09:37, 09:41, 09:46, 09:51
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:36:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:37:00Z"), 100);
        QueryEvent q5 = eventWithDomain(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q6 = eventWithDomain(t("2025-10-02T09:46:00Z"), 100);
        QueryEvent q7 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);
        memoryStatsCollector.receiveNewQuery(q5);
        memoryStatsCollector.receiveNewQuery(q6);
        memoryStatsCollector.receiveNewQuery(q7);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q7, orderedQueryEvents.get(0));
        assertEquals(q6, orderedQueryEvents.get(1));
        assertEquals(q5, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(2);
        assertEquals(1, orderedQueryEvents.size());
        assertEquals(q1, orderedQueryEvents.get(0));
    }

    @Test
    public void getOrderedQueryEventsSkipsEmptyBucketsAndStillPaginatesCorrectly() {
        // Intentionally leave gaps between buckets (no events in some 5-min windows)
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:15:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:55:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T10:05:00Z"), 100); // still within 60-min window at 10:10

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(1);
        assertEquals(1, orderedQueryEvents.size());
        assertEquals(q1, orderedQueryEvents.get(0));
    }

    @Test
    public void getOrderedQueryEventsTrimsEventsOutsideWindowWhenNowAdvances() {
        // Window is 60 minutes. nowRef starts at 10:10.
        // qOld at 09:00 should be outside (70 minutes old) and should not appear after trim.
        QueryEvent qOld = eventWithDomain(t("2025-10-02T09:00:00Z"), 100);
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T10:05:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(qOld);
        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);

        // Trigger trim via getOrderedQueryEvents()
        List<QueryEvent> orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);

        assertFalse(orderedQueryEvents.contains(qOld));
        assertTrue(orderedQueryEvents.contains(q3));

        // Now move "now" forward to make q1 fall outside too:
        // from 10:10 -> 10:40 means anything older than 09:40 is outside.
        nowRef.set(t("2025-10-02T10:40:00Z"));

        orderedQueryEvents = memoryStatsCollector.getOrderedQueryEvents(0);
        assertFalse(orderedQueryEvents.contains(qOld));
        assertFalse(orderedQueryEvents.contains(q1));
        assertTrue(orderedQueryEvents.containsAll(List.of(q3, q2)));
        assertEquals(q3, orderedQueryEvents.get(0));
        assertEquals(q2, orderedQueryEvents.get(1));
    }

    @Test
    public void getOrderedQueryEventsPageZeroDoesNotMutateStateAcrossCalls() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        List<QueryEvent> first = memoryStatsCollector.getOrderedQueryEvents(0);
        List<QueryEvent> second = memoryStatsCollector.getOrderedQueryEvents(0);

        assertEquals(first, second);
        assertEquals(List.of(q4, q3, q2), first);
    }

}
