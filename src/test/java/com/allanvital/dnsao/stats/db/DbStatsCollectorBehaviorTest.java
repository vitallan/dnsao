package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
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

class DbStatsCollectorBehaviorTest {

    @TempDir
    Path tempDir;

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    DbStatsCollector dbStatsCollector;

    @BeforeEach
    void setup() {
        String dbPath = tempDir.resolve("stats.sqlite").toString();
        this.dbStatsCollector = new DbStatsCollector(
                dbPath, 5 * 60_000L, 60 * 60_000L, 25, nowRef::get, 60_000L, 10_000
        );
    }

    @AfterEach
    void teardown() {
        if (dbStatsCollector != null) {
            dbStatsCollector.close();
        }
    }

    @Test
    void shouldCountAnonymizedEvent() throws Exception {
        QueryEvent event = new QueryEvent(QueryResolvedBy.CACHE, null, t("2025-10-02T09:50:00Z"));
        dbStatsCollector.receiveNewQuery(event);
        dbStatsCollector.flushOnce();

        assertEquals(1, dbStatsCollector.getQueryCount(QueryResolvedBy.CACHE));
    }

    @Test
    void shouldCountFullEvent() throws Exception {
        QueryEvent event = new QueryEvent(QueryResolvedBy.UPSTREAM, "1.1.1.1", t("2025-10-02T09:50:00Z"));
        event.setDomain("example.com.");
        event.setClient("192.168.1.1");
        event.setType("A");
        event.setAnswer("10.0.0.1");

        dbStatsCollector.receiveNewQuery(event);
        dbStatsCollector.flushOnce();

        assertEquals(1, dbStatsCollector.getQueryCount(QueryResolvedBy.UPSTREAM));
    }

    @Test
    void shouldPersistFullEventForQueryHistory() throws Exception {
        QueryEvent event = new QueryEvent(QueryResolvedBy.UPSTREAM, "1.1.1.1", t("2025-10-02T09:50:00Z"));
        event.setDomain("example.com.");
        event.setClient("192.168.1.1");
        event.setType("A");
        event.setAnswer("10.0.0.1");
        event.setElapsedTime(42L);

        dbStatsCollector.receiveNewQuery(event);
        dbStatsCollector.flushOnce();

        List<QueryEvent> history = dbStatsCollector.getOrderedQueryEvents();
        assertEquals(1, history.size());
        QueryEvent persisted = history.get(0);
        assertEquals("example.com.", persisted.getDomain());
        assertEquals("192.168.1.1", persisted.getClient());
        assertEquals("A", persisted.getType());
        assertEquals("10.0.0.1", persisted.getAnswer());
        assertEquals(42L, persisted.getElapsedTime());
        assertEquals(1, dbStatsCollector.getQueryCount(QueryResolvedBy.UPSTREAM));
    }

    @Test
    void shouldNotPersistAnonymizedEventForQueryHistory() throws Exception {
        QueryEvent event = new QueryEvent(QueryResolvedBy.CACHE, null, t("2025-10-02T09:50:00Z"));
        dbStatsCollector.receiveNewQuery(event);
        dbStatsCollector.flushOnce();

        List<QueryEvent> history = dbStatsCollector.getOrderedQueryEvents();
        assertTrue(history.isEmpty(),
                "anonymized events should not be persisted for query history");
    }

    @Test
    void shouldTrackUpstreamHitsEvenWhenAnonymized() throws Exception {
        QueryEvent event = new QueryEvent(QueryResolvedBy.UPSTREAM, "8.8.8.8", t("2025-10-02T09:50:00Z"));
        dbStatsCollector.receiveNewQuery(event);
        dbStatsCollector.flushOnce();

        List<QueryEvent> history = dbStatsCollector.getOrderedQueryEvents();
        assertTrue(history.isEmpty(),
                "anonymized upstream event should not appear in query history");
        assertEquals(1L, dbStatsCollector.getQueryCount(QueryResolvedBy.UPSTREAM));
        assertTrue(dbStatsCollector.getUpstreamIndividualHits().containsKey("8.8.8.8"));
        assertEquals(1L, dbStatsCollector.getUpstreamIndividualHits().get("8.8.8.8"));
    }

}
