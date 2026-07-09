package com.allanvital.dnsao.stats.memory;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.PagedQueryResult;
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

    private static QueryEvent eventWithDomainAndClient(long time, String domain, String client) {
        QueryEvent event = new QueryEvent(time, 0);
        event.setDomain(domain);
        event.setClient(client);
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

    @Test
    public void getOrderedQueryEventsWithFilter() {
        QueryEvent q1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "192.168.1.1");
        QueryEvent q2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "test.org.", "192.168.1.2");
        QueryEvent q3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "example.com.", "192.168.1.3");
        QueryEvent q4 = eventWithDomainAndClient(t("2025-10-02T09:34:00Z"), "other.net.", "10.0.0.1");

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        PagedQueryResult result = memoryStatsCollector.getOrderedQueryEvents(0, 25, "example", "time", "desc");
        assertEquals(2, result.total());
        assertEquals(2, result.items().size());
        assertTrue(result.items().containsAll(List.of(q1, q3)));

        result = memoryStatsCollector.getOrderedQueryEvents(0, 25, "192.168", "time", "desc");
        assertEquals(3, result.total());
        assertEquals(3, result.items().size());
    }

    @Test
    public void getOrderedQueryEventsWithSortAscending() {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "alpha.com.", "1.1.1.1");
        QueryEvent e2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "beta.org.", "2.2.2.2");
        QueryEvent e3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "gamma.net.", "3.3.3.3");

        memoryStatsCollector.receiveNewQuery(e1);
        memoryStatsCollector.receiveNewQuery(e2);
        memoryStatsCollector.receiveNewQuery(e3);

        PagedQueryResult result = memoryStatsCollector.getOrderedQueryEvents(0, 25, "", "time", "asc");
        assertEquals(3, result.total());
        assertEquals(e1, result.items().get(0));
        assertEquals(e2, result.items().get(1));
        assertEquals(e3, result.items().get(2));
    }

    @Test
    public void getOrderedQueryEventsWithSortByDomain() {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "gamma.net.", "1.1.1.1");
        QueryEvent e2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "alpha.com.", "2.2.2.2");
        QueryEvent e3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "beta.org.", "3.3.3.3");

        memoryStatsCollector.receiveNewQuery(e1);
        memoryStatsCollector.receiveNewQuery(e2);
        memoryStatsCollector.receiveNewQuery(e3);

        PagedQueryResult result = memoryStatsCollector.getOrderedQueryEvents(0, 25, "", "domain", "asc");
        assertEquals(3, result.total());
        assertEquals("alpha.com.", result.items().get(0).getDomain());
        assertEquals("beta.org.", result.items().get(1).getDomain());
        assertEquals("gamma.net.", result.items().get(2).getDomain());
    }

    @Test
    public void getOrderedQueryEventsFilteredAndPaginated() {
        long base = t("2025-10-02T09:31:00Z");
        for (int i = 0; i < 10; i++) {
            QueryEvent ev = eventWithDomainAndClient(base + i * 1000, "search.me.", "10.0.0." + (i + 1));
            memoryStatsCollector.receiveNewQuery(ev);
        }

        PagedQueryResult page0 = memoryStatsCollector.getOrderedQueryEvents(0, 3, "search.me", "time", "desc");
        assertEquals(10, page0.total());
        assertEquals(3, page0.items().size());
        assertEquals("10.0.0.10", page0.items().get(0).getClient());

        PagedQueryResult page1 = memoryStatsCollector.getOrderedQueryEvents(1, 3, "search.me", "time", "desc");
        assertEquals(10, page1.total());
        assertEquals(3, page1.items().size());
    }

    @Test
    public void getOrderedQueryEventsWithFilterNoMatch() {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "1.1.1.1");
        memoryStatsCollector.receiveNewQuery(e1);

        PagedQueryResult result = memoryStatsCollector.getOrderedQueryEvents(0, 25, "nonexistent", "time", "desc");
        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

    @Test
    public void getOrderedQueryEventsWithNegativePage() {
        QueryEvent e = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "1.1.1.1");
        memoryStatsCollector.receiveNewQuery(e);

        PagedQueryResult result = memoryStatsCollector.getOrderedQueryEvents(-1, 25, "", "time", "desc");
        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

}
