package com.allanvital.dnsao.stats;

import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.StatsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IndividualStatsCollectionTest {

    StatsCollector statsCollector;

    @BeforeEach
    public void setup() {
        statsCollector = new StatsCollector();
    }

    @Test
    public void testEachQueryResolvedByEventOptionAndValidateCounter() {
        for (QueryResolvedBy queryResolvedBy : QueryResolvedBy.values()) {
            doTest(queryResolvedBy);
        }
    }

    private void doTest(QueryResolvedBy queryResolvedBy) {
        assertEquals(0, statsCollector.getQueryCount(queryResolvedBy));
        statsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy));
        assertEquals(1, statsCollector.getQueryCount(queryResolvedBy));
        statsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy));
        assertEquals(2, statsCollector.getQueryCount(queryResolvedBy));
    }

}
