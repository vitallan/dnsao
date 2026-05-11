package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbUpstreamHitCountTest {

    @TempDir
    Path tempDir;

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
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
    public void testCountOnUpstreamSummarizationOnlyOnCurrentWindow() throws Exception {
        dbStatsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:07:00Z")));
        dbStatsCollector.receiveNewQuery(new QueryEvent(UPSTREAM, "1.1.1.1", t("2025-10-02T10:08:00Z")));
        dbStatsCollector.flushOnce();

        Map<String, Long> upstreamHits = dbStatsCollector.getUpstreamIndividualHits();
        assertEquals(2, upstreamHits.get("1.1.1.1"));

        nowRef.set(t("2025-10-02T12:10:00Z"));
        dbStatsCollector.flushOnce();

        upstreamHits = dbStatsCollector.getUpstreamIndividualHits();
        assertEquals(0, upstreamHits.getOrDefault("1.1.1.1", 0L));
    }
}
