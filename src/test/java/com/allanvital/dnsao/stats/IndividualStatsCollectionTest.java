package com.allanvital.dnsao.stats;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.memory.MemoryStatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualStatsCollectionTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    MemoryStatsCollector memoryStatsCollector;

    @BeforeEach
    public void setup() {
        this.memoryStatsCollector = new MemoryStatsCollector(5 * 60_000L, 60 * 60_000L, nowRef::get);
    }

    @Test
    public void testEachQueryResolvedByEventOptionAndValidateCounter() {
        for (QueryResolvedBy queryResolvedBy : QueryResolvedBy.values()) {
            doTest(queryResolvedBy);
        }
    }

    private void doTest(QueryResolvedBy queryResolvedBy) {
        assertEquals(0, memoryStatsCollector.getQueryCount(queryResolvedBy));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        assertEquals(1, memoryStatsCollector.getQueryCount(queryResolvedBy));
        memoryStatsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        assertEquals(2, memoryStatsCollector.getQueryCount(queryResolvedBy));
    }

}
