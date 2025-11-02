package com.allanvital.dnsao.stats;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.StatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.TestHolder.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualStatsCollectionTest {

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    StatsCollector statsCollector;

    @BeforeEach
    public void setup() {
        this.statsCollector = new StatsCollector(5 * 60_000L, 60 * 60_000L, nowRef::get);
    }

    @Test
    public void testEachQueryResolvedByEventOptionAndValidateCounter() {
        for (QueryResolvedBy queryResolvedBy : QueryResolvedBy.values()) {
            doTest(queryResolvedBy);
        }
    }

    private void doTest(QueryResolvedBy queryResolvedBy) {
        assertEquals(0, statsCollector.getQueryCount(queryResolvedBy));
        statsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        assertEquals(1, statsCollector.getQueryCount(queryResolvedBy));
        statsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        assertEquals(2, statsCollector.getQueryCount(queryResolvedBy));
    }

}
