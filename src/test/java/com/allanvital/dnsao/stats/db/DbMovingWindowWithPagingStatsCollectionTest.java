package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.PagedQueryResult;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.*;

public class DbMovingWindowWithPagingStatsCollectionTest {

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

    @TempDir
    Path tempDir;

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:10:00Z"));
    int pageSize = 3;
    DbStatsCollector dbStatsCollector;

    @BeforeEach
    public void setup() {
        String dbPath = tempDir.resolve("stats.sqlite").toString();
        // 60 min / 5 min window = 12 buckets
        this.dbStatsCollector = new DbStatsCollector(dbPath, 5 * 60_000L, 60 * 60_000L, pageSize, nowRef::get, 60_000L, 10_000);
    }

    @AfterEach
    public void teardown() {
        if (dbStatsCollector != null) {
            dbStatsCollector.close();
        }
    }

    @Test
    public void getOrderedQueryEventsWithPages() throws Exception {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:51:00Z"), 100);

        dbStatsCollector.receiveNewQuery(q1);
        dbStatsCollector.receiveNewQuery(q2);
        dbStatsCollector.receiveNewQuery(q3);
        dbStatsCollector.receiveNewQuery(q4);
        dbStatsCollector.flushOnce();

        List<QueryEvent> orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertFalse(orderedQueryEvents.contains(q1));
        assertTrue(orderedQueryEvents.containsAll(List.of(q4, q3, q2)));
        assertEquals(q4, orderedQueryEvents.get(0));
        assertEquals(q3, orderedQueryEvents.get(1));
        assertEquals(q2, orderedQueryEvents.get(2));

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(1);
        assertEquals(1, orderedQueryEvents.size());
        assertTrue(orderedQueryEvents.contains(q1));
        assertEquals(q1, orderedQueryEvents.get(0));
    }

    @Test
    public void getOrderedQueryEventsEmptyCollectorReturnsEmpty() {
        List<QueryEvent> orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(1);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsPageBeyondAvailableReturnsEmpty() throws Exception {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);

        dbStatsCollector.receiveNewQuery(q1);
        dbStatsCollector.receiveNewQuery(q2);
        dbStatsCollector.flushOnce();

        List<QueryEvent> orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertEquals(2, orderedQueryEvents.size());
        assertEquals(q2, orderedQueryEvents.get(0));
        assertEquals(q1, orderedQueryEvents.get(1));

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(1);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(10);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsExactlyMultipleOfPageSize() throws Exception {
        QueryEvent q1 = eventWithDomain(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = eventWithDomain(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = eventWithDomain(t("2025-10-02T09:33:00Z"), 100);
        QueryEvent q4 = eventWithDomain(t("2025-10-02T09:34:00Z"), 100);
        QueryEvent q5 = eventWithDomain(t("2025-10-02T09:35:00Z"), 100);
        QueryEvent q6 = eventWithDomain(t("2025-10-02T09:36:00Z"), 100);

        dbStatsCollector.receiveNewQuery(q1);
        dbStatsCollector.receiveNewQuery(q2);
        dbStatsCollector.receiveNewQuery(q3);
        dbStatsCollector.receiveNewQuery(q4);
        dbStatsCollector.receiveNewQuery(q5);
        dbStatsCollector.receiveNewQuery(q6);
        dbStatsCollector.flushOnce();

        List<QueryEvent> orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q6, orderedQueryEvents.get(0));
        assertEquals(q5, orderedQueryEvents.get(1));
        assertEquals(q4, orderedQueryEvents.get(2));

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(1);
        assertEquals(3, orderedQueryEvents.size());
        assertEquals(q3, orderedQueryEvents.get(0));
        assertEquals(q2, orderedQueryEvents.get(1));
        assertEquals(q1, orderedQueryEvents.get(2));

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(2);
        assertNotNull(orderedQueryEvents);
        assertTrue(orderedQueryEvents.isEmpty());
    }

    @Test
    public void getOrderedQueryEventsWithFilter() throws Exception {
        QueryEvent q1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "192.168.1.1");
        QueryEvent q2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "test.org.", "192.168.1.2");
        QueryEvent q3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "example.com.", "192.168.1.3");
        QueryEvent q4 = eventWithDomainAndClient(t("2025-10-02T09:34:00Z"), "other.net.", "10.0.0.1");

        dbStatsCollector.receiveNewQuery(q1);
        dbStatsCollector.receiveNewQuery(q2);
        dbStatsCollector.receiveNewQuery(q3);
        dbStatsCollector.receiveNewQuery(q4);
        dbStatsCollector.flushOnce();

        PagedQueryResult result = dbStatsCollector.getOrderedQueryEvents(0, 25, "example", "time", "desc");
        assertEquals(2, result.total());
        assertEquals(2, result.items().size());
        assertTrue(result.items().containsAll(List.of(q1, q3)));

        result = dbStatsCollector.getOrderedQueryEvents(0, 25, "192.168", "time", "desc");
        assertEquals(3, result.total());
        assertEquals(3, result.items().size());
    }

    @Test
    public void getOrderedQueryEventsWithSortAscending() throws Exception {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "alpha.com.", "1.1.1.1");
        QueryEvent e2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "beta.org.", "2.2.2.2");
        QueryEvent e3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "gamma.net.", "3.3.3.3");

        dbStatsCollector.receiveNewQuery(e1);
        dbStatsCollector.receiveNewQuery(e2);
        dbStatsCollector.receiveNewQuery(e3);
        dbStatsCollector.flushOnce();

        PagedQueryResult result = dbStatsCollector.getOrderedQueryEvents(0, 25, "", "time", "asc");
        assertEquals(3, result.total());
        assertEquals(e1, result.items().get(0));
        assertEquals(e2, result.items().get(1));
        assertEquals(e3, result.items().get(2));
    }

    @Test
    public void getOrderedQueryEventsWithSortByDomain() throws Exception {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "gamma.net.", "1.1.1.1");
        QueryEvent e2 = eventWithDomainAndClient(t("2025-10-02T09:32:00Z"), "alpha.com.", "2.2.2.2");
        QueryEvent e3 = eventWithDomainAndClient(t("2025-10-02T09:33:00Z"), "beta.org.", "3.3.3.3");

        dbStatsCollector.receiveNewQuery(e1);
        dbStatsCollector.receiveNewQuery(e2);
        dbStatsCollector.receiveNewQuery(e3);
        dbStatsCollector.flushOnce();

        PagedQueryResult result = dbStatsCollector.getOrderedQueryEvents(0, 25, "", "domain", "asc");
        assertEquals(3, result.total());
        assertEquals("alpha.com.", result.items().get(0).getDomain());
        assertEquals("beta.org.", result.items().get(1).getDomain());
        assertEquals("gamma.net.", result.items().get(2).getDomain());
    }

    @Test
    public void getOrderedQueryEventsFilteredAndPaginated() throws Exception {
        long base = t("2025-10-02T09:31:00Z");
        for (int i = 0; i < 10; i++) {
            QueryEvent ev = eventWithDomainAndClient(base + i * 1000, "search.me.", "10.0.0." + (i + 1));
            dbStatsCollector.receiveNewQuery(ev);
        }
        dbStatsCollector.flushOnce();

        PagedQueryResult page0 = dbStatsCollector.getOrderedQueryEvents(0, 3, "search.me", "time", "desc");
        assertEquals(10, page0.total());
        assertEquals(3, page0.items().size());
        assertEquals("10.0.0.10", page0.items().get(0).getClient());

        PagedQueryResult page1 = dbStatsCollector.getOrderedQueryEvents(1, 3, "search.me", "time", "desc");
        assertEquals(10, page1.total());
        assertEquals(3, page1.items().size());
    }

    @Test
    public void getOrderedQueryEventsWithFilterNoMatch() throws Exception {
        QueryEvent e1 = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "1.1.1.1");
        dbStatsCollector.receiveNewQuery(e1);
        dbStatsCollector.flushOnce();

        PagedQueryResult result = dbStatsCollector.getOrderedQueryEvents(0, 25, "nonexistent", "time", "desc");
        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

    @Test
    public void getOrderedQueryEventsWithNegativePage() throws Exception {
        QueryEvent e = eventWithDomainAndClient(t("2025-10-02T09:31:00Z"), "example.com.", "1.1.1.1");
        dbStatsCollector.receiveNewQuery(e);
        dbStatsCollector.flushOnce();

        PagedQueryResult result = dbStatsCollector.getOrderedQueryEvents(-1, 25, "", "time", "desc");
        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

}
