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
        this.memoryStatsCollector = new MemoryStatsCollector(5 * 60_000L, 60 * 60_000L, pageSize, nowRef::get);
    }

    private PagedQueryResult result(int p) {
        return memoryStatsCollector.getOrderedQueryEvents(p, pageSize, "", "time", "desc");
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

        PagedQueryResult r0 = result(0);
        assertEquals(3, r0.items().size());
        assertFalse(r0.items().contains(q1));
        assertTrue(r0.items().containsAll(List.of(q4, q3, q2)));
        assertEquals(q4, r0.items().get(0));
        assertEquals(q3, r0.items().get(1));
        assertEquals(q2, r0.items().get(2));

        PagedQueryResult r1 = result(1);
        assertEquals(1, r1.items().size());
        assertTrue(r1.items().contains(q1));
        assertEquals(q1, r1.items().get(0));
    }

    @Test
    public void getOrderedQueryEventsEmptyCollectorReturnsEmpty() {
        PagedQueryResult r0 = result(0);
        assertNotNull(r0.items());
        assertTrue(r0.items().isEmpty());

        PagedQueryResult r1 = result(1);
        assertNotNull(r1.items());
        assertTrue(r1.items().isEmpty());
    }

    @Test
    public void getOrderedQueryEventsPageBeyondAvailableReturnsEmpty() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);

        PagedQueryResult r0 = result(0);
        assertEquals(2, r0.items().size());
        assertEquals(q2, r0.items().get(0));
        assertEquals(q1, r0.items().get(1));

        PagedQueryResult r1 = result(1);
        assertNotNull(r1.items());
        assertTrue(r1.items().isEmpty());

        PagedQueryResult r10 = result(10);
        assertNotNull(r10.items());
        assertTrue(r10.items().isEmpty());
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

        PagedQueryResult r0 = result(0);
        assertEquals(3, r0.items().size());
        assertEquals(q6, r0.items().get(0));
        assertEquals(q5, r0.items().get(1));
        assertEquals(q4, r0.items().get(2));

        PagedQueryResult r1 = result(1);
        assertEquals(3, r1.items().size());
        assertEquals(q3, r1.items().get(0));
        assertEquals(q2, r1.items().get(1));
        assertEquals(q1, r1.items().get(2));

        PagedQueryResult r2 = result(2);
        assertNotNull(r2.items());
        assertTrue(r2.items().isEmpty());
    }

    @Test
    public void getOrderedQueryEventsOrdersCorrectlyWithinSameBucket() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:34:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:33:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        PagedQueryResult r0 = result(0);
        assertEquals(3, r0.items().size());
        assertEquals(q2, r0.items().get(0));
        assertEquals(q3, r0.items().get(1));
        assertEquals(q1, r0.items().get(2));

        PagedQueryResult r1 = result(1);
        assertEquals(1, r1.items().size());
        assertEquals(q4, r1.items().get(0));
    }

    @Test
    public void getOrderedQueryEventsOrdersCorrectlyAcrossBucketBoundaries() {
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

        PagedQueryResult r0 = result(0);
        assertEquals(3, r0.items().size());
        assertEquals(q7, r0.items().get(0));
        assertEquals(q6, r0.items().get(1));
        assertEquals(q5, r0.items().get(2));

        PagedQueryResult r1 = result(1);
        assertEquals(3, r1.items().size());
        assertEquals(q4, r1.items().get(0));
        assertEquals(q3, r1.items().get(1));
        assertEquals(q2, r1.items().get(2));

        PagedQueryResult r2 = result(2);
        assertEquals(1, r2.items().size());
        assertEquals(q1, r2.items().get(0));
    }

    @Test
    public void getOrderedQueryEventsSkipsEmptyBucketsAndStillPaginatesCorrectly() {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:15:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:55:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T10:05:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);
        memoryStatsCollector.receiveNewQuery(q4);

        PagedQueryResult r0 = result(0);
        assertEquals(3, r0.items().size());
        assertEquals(q4, r0.items().get(0));
        assertEquals(q3, r0.items().get(1));
        assertEquals(q2, r0.items().get(2));

        PagedQueryResult r1 = result(1);
        assertEquals(1, r1.items().size());
        assertEquals(q1, r1.items().get(0));
    }

    @Test
    public void getOrderedQueryEventsTrimsEventsOutsideWindowWhenNowAdvances() {
        QueryEvent qOld = eventWithDomain(t("2025-10-02T09:00:00Z"), 100);
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T10:05:00Z"), 100);

        memoryStatsCollector.receiveNewQuery(qOld);
        memoryStatsCollector.receiveNewQuery(q1);
        memoryStatsCollector.receiveNewQuery(q2);
        memoryStatsCollector.receiveNewQuery(q3);

        PagedQueryResult r0 = result(0);
        assertFalse(r0.items().contains(qOld));
        assertTrue(r0.items().contains(q3));

        nowRef.set(t("2025-10-02T10:40:00Z"));

        PagedQueryResult r1 = result(0);
        assertFalse(r1.items().contains(qOld));
        assertFalse(r1.items().contains(q1));
        assertTrue(r1.items().containsAll(List.of(q3, q2)));
        assertEquals(q3, r1.items().get(0));
        assertEquals(q2, r1.items().get(1));
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

        PagedQueryResult first = result(0);
        PagedQueryResult second = result(0);

        assertEquals(first.items(), second.items());
        assertEquals(List.of(q4, q3, q2), first.items());
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