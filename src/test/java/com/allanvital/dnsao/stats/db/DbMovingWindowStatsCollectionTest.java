package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.Bucket;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.*;

public class DbMovingWindowStatsCollectionTest {

    @TempDir
    Path tempDir;

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:10:00Z"));
    DbStatsCollector dbStatsCollector;

    @BeforeEach
    public void setup() {
        String dbPath = tempDir.resolve("stats.sqlite").toString();
        // 60 min / 5 min window = 12 buckets
        this.dbStatsCollector = new DbStatsCollector(dbPath, 5 * 60_000L, 60 * 60_000L, 25, nowRef::get, 60_000L, 10_000);
    }

    @AfterEach
    public void teardown() {
        if (dbStatsCollector != null) {
            dbStatsCollector.close();
        }
    }

    @Test
    public void filledAnchoredToNowReturnsContinuousSeriesWithZeros() throws Exception {
        AtomicLong customRef = new AtomicLong(t("2025-10-02T10:25:00Z"));
        String dbPath = tempDir.resolve("stats-anchored.sqlite").toString();
        DbStatsCollector collector = new DbStatsCollector(dbPath, 5 * 60_000L, 20 * 60_000L, 25, customRef::get, 60_000L, 10_000);
        try {
            collector.receiveNewQuery(new QueryEvent(t("2025-10-02T10:10:01Z")));
            collector.flushOnce();

            Map<Long, Bucket> filled = collector.getBucketsFilledAnchoredToNow();
            assertEquals(4, filled.size());

            long[] expectedStarts = {
                    t("2025-10-02T10:10:00Z"),
                    t("2025-10-02T10:15:00Z"),
                    t("2025-10-02T10:20:00Z"),
                    t("2025-10-02T10:25:00Z")
            };
            long[] expectedVals = {1, 0, 0, 0};

            for (int i = 0; i < expectedStarts.length; i++) {
                Bucket b = filled.get(expectedStarts[i]);
                assertNotNull(b, "missing bucket idx=" + i);
                assertEquals(expectedVals[i], b.getTotalCounter(), "unexpected value idx=" + i);
            }
        } finally {
            collector.close();
        }
    }

    @Test
    public void getOrderedQueryEventsTrimsEventsOutsideWindowWhenNowAdvances() throws Exception {
        // Window is 60 minutes. nowRef starts at 10:10.
        QueryEvent qOld = new QueryEvent(t("2025-10-02T09:00:00Z"), 100);
        QueryEvent q1 = new QueryEvent(t("2025-10-02T09:31:00Z"), 100);
        QueryEvent q2 = new QueryEvent(t("2025-10-02T09:51:00Z"), 100);
        QueryEvent q3 = new QueryEvent(t("2025-10-02T10:05:00Z"), 100);

        dbStatsCollector.receiveNewQuery(qOld);
        dbStatsCollector.receiveNewQuery(q1);
        dbStatsCollector.receiveNewQuery(q2);
        dbStatsCollector.receiveNewQuery(q3);
        dbStatsCollector.flushOnce();

        List<QueryEvent> orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertFalse(orderedQueryEvents.contains(qOld));
        assertTrue(orderedQueryEvents.contains(q3));

        nowRef.set(t("2025-10-02T10:40:00Z"));
        dbStatsCollector.flushOnce();

        orderedQueryEvents = dbStatsCollector.getOrderedQueryEvents(0);
        assertFalse(orderedQueryEvents.contains(qOld));
        assertFalse(orderedQueryEvents.contains(q1));
        assertTrue(orderedQueryEvents.containsAll(List.of(q3, q2)));
        assertEquals(q3, orderedQueryEvents.get(0));
        assertEquals(q2, orderedQueryEvents.get(1));
    }
}
