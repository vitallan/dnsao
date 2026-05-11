package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbIndividualStatsCollectionTest {

    @TempDir
    Path tempDir;

    AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));
    DbStatsCollector dbStatsCollector;

    @BeforeEach
    public void setup() {
        String dbPath = tempDir.resolve("stats.sqlite").toString();
        this.dbStatsCollector = new DbStatsCollector(dbPath, 5 * 60_000L, 60 * 60_000L, 25, nowRef::get, 60_000L, 10_000);
    }

    @AfterEach
    public void teardown() {
        if (dbStatsCollector != null) {
            dbStatsCollector.close();
        }
    }

    @Test
    public void testEachQueryResolvedByEventOptionAndValidateCounter() throws Exception {
        for (QueryResolvedBy queryResolvedBy : QueryResolvedBy.values()) {
            doTest(queryResolvedBy);
        }
    }

    private void doTest(QueryResolvedBy queryResolvedBy) throws Exception {
        assertEquals(0, dbStatsCollector.getQueryCount(queryResolvedBy));
        dbStatsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        dbStatsCollector.receiveNewQuery(new QueryEvent(queryResolvedBy, null, t("2025-10-02T09:50:00Z")));
        dbStatsCollector.flushOnce();
        assertEquals(2, dbStatsCollector.getQueryCount(queryResolvedBy));
    }
}
