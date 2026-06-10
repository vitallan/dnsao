package com.allanvital.dnsao.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.allanvital.dnsao.holder.TestHolder.t;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaMigrationTest {

    @TempDir
    private Path tempDir;

    private Path dbPath;
    private DbStatsCollector collector;
    private final String DB_NAME = "old-stats.sqlite";

    private AtomicLong nowRef = new AtomicLong(t("2025-10-02T10:00:00Z"));

    @BeforeEach
    public void setup() throws Exception{
        dbPath = tempDir.resolve(DB_NAME);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE bucket_agg (" +
                    "bucket_start_ms INTEGER PRIMARY KEY," +
                    "total INTEGER NOT NULL," +
                    "cache INTEGER NOT NULL," +
                    "blocked INTEGER NOT NULL," +
                    "local INTEGER NOT NULL," +
                    "upstream INTEGER NOT NULL," +
                    "refused INTEGER NOT NULL," +
                    "servfail INTEGER NOT NULL," +
                    "elapsed_sum INTEGER NOT NULL" +
                    ")");
        }
        collector = new DbStatsCollector(dbPath.toString(), 5 * 60_000L, 60 * 60_000L, 25, nowRef::get, 60_000L, 10_000);
    }

    @Test
    public void upgradesOldSchemaWhenMissingRecursionColumn() throws Exception {
        collector.receiveNewQuery(new QueryEvent(QueryResolvedBy.RECURSION, null, t("2025-10-02T09:50:00Z")));
        collector.flushOnce();

        assertEquals(1, collector.getQueryCount(QueryResolvedBy.RECURSION));
    }

    @Test
    public void nonRecursionEventsStillWorkAfterMigration() throws Exception {
        collector.receiveNewQuery(new QueryEvent(QueryResolvedBy.CACHE, null, t("2025-10-02T09:50:00Z")));
        collector.receiveNewQuery(new QueryEvent(QueryResolvedBy.RECURSION, null, t("2025-10-02T09:50:01Z")));
        collector.receiveNewQuery(new QueryEvent(QueryResolvedBy.BLOCKED, null, t("2025-10-02T09:50:02Z")));
        collector.flushOnce();

        assertEquals(1, collector.getQueryCount(QueryResolvedBy.CACHE));
        assertEquals(1, collector.getQueryCount(QueryResolvedBy.RECURSION));
        assertEquals(1, collector.getQueryCount(QueryResolvedBy.BLOCKED));
    }

    @AfterEach
    public void tearDown() {
        cleanDbFiles();
        collector.close();
    }

    private void cleanDbFiles() {
        try (Stream<Path> files = Files.list(tempDir)) {
            files.forEach(f -> { try { Files.deleteIfExists(f); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }

}
