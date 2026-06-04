package com.allanvital.dnsao.web.stats.memory;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.*;

class MemoryStatsCollectorBehaviorTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    MemoryStatsCollector memoryStatsCollector;

    @BeforeEach
    void setup() {
        this.memoryStatsCollector = new MemoryStatsCollector(
                5 * 60_000L, 60 * 60_000L, nowRef::get
        );
    }

    @Test
    void shouldCountEventWithMinimalFields() {
        QueryEvent minimal = new QueryEvent(QueryResolvedBy.CACHE, null, t("2025-10-02T09:50:00Z"));
        memoryStatsCollector.receiveNewQuery(minimal);

        assertEquals(1, memoryStatsCollector.getQueryCount(QueryResolvedBy.CACHE));
    }

    @Test
    void shouldNotStoreMinimalEventForHistory() {
        QueryEvent minimal = new QueryEvent(QueryResolvedBy.CACHE, null, t("2025-10-02T09:50:00Z"));
        memoryStatsCollector.receiveNewQuery(minimal);

        List<QueryEvent> events = memoryStatsCollector.getOrderedQueryEvents();
        assertTrue(events.isEmpty(), "anonymized events should not appear in query history");
    }

    @Test
    void shouldStoreFullEventForHistory() {
        QueryEvent full = new QueryEvent(QueryResolvedBy.UPSTREAM, "1.1.1.1", t("2025-10-02T09:50:00Z"));
        full.setDomain("example.com.");
        full.setClient("192.168.1.1");
        full.setType("A");
        full.setAnswer("10.0.0.1");

        memoryStatsCollector.receiveNewQuery(full);

        assertEquals(1, memoryStatsCollector.getQueryCount(QueryResolvedBy.UPSTREAM));
        List<QueryEvent> events = memoryStatsCollector.getOrderedQueryEvents();
        assertEquals(1, events.size());
        assertEquals("example.com.", events.get(0).getDomain());
    }

}
