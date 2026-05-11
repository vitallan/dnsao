package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
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
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = new QueryEvent(t("2025-10-02T09:41:00Z"), 100);
        QueryEvent q4 = new QueryEvent(t("2025-10-02T09:51:00Z"), 100);

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
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:32:00Z"), 100);

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
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:32:00Z"), 100);
        QueryEvent q3 = new QueryEvent(t("2025-10-02T09:33:00Z"), 100);
        QueryEvent q4 = new QueryEvent(t("2025-10-02T09:34:00Z"), 100);
        QueryEvent q5 = new QueryEvent(t("2025-10-02T09:35:00Z"), 100);
        QueryEvent q6 = new QueryEvent(t("2025-10-02T09:36:00Z"), 100);

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
}
