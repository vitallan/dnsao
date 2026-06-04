package com.allanvital.dnsao.web.stats.db;

import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryEventListener;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.Bucket;
import com.allanvital.dnsao.web.stats.StatsCollector;
import com.allanvital.dnsao.web.stats.memory.MemoryStatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

public class DbStatsCollector implements StatsCollector, QueryEventListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    public static final long DEFAULT_BUCKET_INTERVAL_MS = MemoryStatsCollector.DEFAULT_BUCKET_INTERVAL_MS;
    public static final long DEFAULT_WINDOW_MS = MemoryStatsCollector.DEFAULT_WINDOW_MS;
    public static final int DEFAULT_PAGE_SIZE = MemoryStatsCollector.DEFAULT_PAGE_SIZE;
    public static final long DEFAULT_FLUSH_INTERVAL_MS = 500L;

    private final long bucketIntervalMs;
    private final long windowMs;
    private final int maxBuckets;
    private final int pageSize;
    private final LongSupplier nowSupplier;
    private final long flushIntervalMs;

    private final Path dbPath;

    private final int queueCapacity;
    private final ArrayBlockingQueue<QueryEvent> queue;

    private final ScheduledExecutorService writerExecutor;
    private volatile boolean closed = false;

    private Connection writerConn;
    private Connection readConn;

    public DbStatsCollector(String dbPath) {
        this(dbPath,
                DEFAULT_BUCKET_INTERVAL_MS,
                DEFAULT_WINDOW_MS,
                DEFAULT_PAGE_SIZE,
                Clock::currentTimeInMillis,
                DEFAULT_FLUSH_INTERVAL_MS,
                50_000);
    }

    public DbStatsCollector(String dbPath,
                            long bucketIntervalMs,
                            long windowMs,
                            int pageSize,
                            LongSupplier nowSupplier,
                            long flushIntervalMs,
                            int queueCapacity) {
        this.bucketIntervalMs = bucketIntervalMs;
        this.windowMs = windowMs;
        this.pageSize = pageSize;
        this.nowSupplier = nowSupplier;
        this.flushIntervalMs = flushIntervalMs;
        this.maxBuckets = Math.max(1, (int) (windowMs / bucketIntervalMs));
        this.queueCapacity = Math.max(1, queueCapacity);
        this.queue = new ArrayBlockingQueue<>(this.queueCapacity);
        this.dbPath = Paths.get(Objects.requireNonNull(dbPath, "dbPath")).toAbsolutePath().normalize();

        validateDbPathFailFast(this.dbPath);

        try {
            initConnectionsAndSchema();
        } catch (SQLException e) {
            throw new RuntimeException("failed initializing sqlite stats DB at " + this.dbPath, e);
        }

        this.writerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stats-db-writer");
            t.setDaemon(true);
            return t;
        });

        this.writerExecutor.scheduleWithFixedDelay(this::flushOnceSafe,
                this.flushIntervalMs,
                this.flushIntervalMs,
                TimeUnit.MILLISECONDS);
    }

    private static void validateDbPathFailFast(Path dbPath) {
        Path parent = dbPath.getParent();
        if (parent == null || !Files.exists(parent)) {
            throw new IllegalArgumentException("statsDbPath parent directory does not exist: " + parent);
        }
        if (!Files.isDirectory(parent)) {
            throw new IllegalArgumentException("statsDbPath parent is not a directory: " + parent);
        }
        if (!Files.isWritable(parent)) {
            throw new IllegalArgumentException("statsDbPath parent directory is not writable: " + parent);
        }
        // If DB already exists, ensure it's writable.
        if (Files.exists(dbPath) && !Files.isWritable(dbPath)) {
            throw new IllegalArgumentException("statsDbPath is not writable: " + dbPath);
        }
    }

    private void initConnectionsAndSchema() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        this.writerConn = DriverManager.getConnection(url);
        this.readConn = DriverManager.getConnection(url);

        try (Statement st = writerConn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA temp_store=MEMORY");
        }

        try (Statement st = writerConn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS query_event (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "time_ms INTEGER NOT NULL," +
                    "resolved_by TEXT," +
                    "client TEXT," +
                    "type TEXT," +
                    "domain TEXT," +
                    "answer TEXT," +
                    "source TEXT," +
                    "elapsed_ms INTEGER" +
                    ")");

            st.execute("CREATE INDEX IF NOT EXISTS idx_query_event_time ON query_event(time_ms DESC, id DESC)");

            st.execute("CREATE TABLE IF NOT EXISTS bucket_agg (" +
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

            st.execute("CREATE TABLE IF NOT EXISTS upstream_hit (" +
                    "bucket_start_ms INTEGER NOT NULL," +
                    "upstream TEXT NOT NULL," +
                    "hits INTEGER NOT NULL," +
                    "PRIMARY KEY(bucket_start_ms, upstream)" +
                    ")");
        }
    }

    @Override
    public void receiveNewQuery(QueryEvent queryEvent) {
        if (closed) {
            return;
        }

        // drop oldest events when full.
        if (!queue.offer(queryEvent)) {
            queue.poll();
            queue.offer(queryEvent);
        }
    }

    @Override
    public Map<String, Long> getUpstreamIndividualHits() {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long first = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        String sql = "SELECT upstream, SUM(hits) AS total_hits " +
                "FROM upstream_hit WHERE bucket_start_ms >= ? AND bucket_start_ms <= ? GROUP BY upstream";

        Map<String, Long> out = new HashMap<>();
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, first);
            ps.setLong(2, latestAllowedBucket);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getLong(2));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Override
    public Map<Long, Bucket> getBucketsFilledAnchoredToNow() {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long first = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        Map<Long, DbBucket> byStart = new HashMap<>();
        String sql = "SELECT bucket_start_ms, total, cache, blocked, local, upstream, refused, servfail " +
                "FROM bucket_agg WHERE bucket_start_ms >= ? AND bucket_start_ms <= ?";
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, first);
            ps.setLong(2, latestAllowedBucket);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long start = rs.getLong(1);
                    byStart.put(start, new DbBucket(
                            rs.getLong(2),
                            rs.getLong(3),
                            rs.getLong(4),
                            rs.getLong(5),
                            rs.getLong(6),
                            rs.getLong(7),
                            rs.getLong(8)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Map<Long, Bucket> out = new HashMap<>();
        for (long t = first; t <= latestAllowedBucket; t += bucketIntervalMs) {
            DbBucket b = byStart.get(t);
            out.put(t, (b == null) ? DbBucket.zero() : b);
        }
        return out;
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents() {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long earliestAllowed = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;
        long latestAllowed = Math.max(now, latestAllowedBucket + bucketIntervalMs - 1);

        String sql = "SELECT time_ms, resolved_by, client, type, domain, answer, source, elapsed_ms " +
                "FROM query_event WHERE time_ms >= ? AND time_ms <= ? ORDER BY time_ms DESC, id DESC";

        List<QueryEvent> out = new ArrayList<>();
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, earliestAllowed);
            ps.setLong(2, latestAllowed);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readQueryEventRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents(int page) {
        if (page < 0) {
            return List.of();
        }

        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long earliestAllowed = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;
        long latestAllowed = Math.max(now, latestAllowedBucket + bucketIntervalMs - 1);

        String sql = "SELECT time_ms, resolved_by, client, type, domain, answer, source, elapsed_ms " +
                "FROM query_event WHERE time_ms >= ? AND time_ms <= ? " +
                "ORDER BY time_ms DESC, id DESC LIMIT ? OFFSET ?";

        int offset = page * pageSize;
        List<QueryEvent> out = new ArrayList<>();
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, earliestAllowed);
            ps.setLong(2, latestAllowed);
            ps.setInt(3, pageSize);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readQueryEventRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Override
    public double getQueryElapsedTime() {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long first = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        String sql = "SELECT SUM(elapsed_sum) AS s, SUM(total) AS c FROM bucket_agg " +
                "WHERE bucket_start_ms >= ? AND bucket_start_ms <= ?";
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, first);
            ps.setLong(2, latestAllowedBucket);
            try (ResultSet rs = ps.executeQuery()) {
                long sum = rs.getLong(1);
                long count = rs.getLong(2);
                if (count == 0) {
                    return 0.0;
                }
                return sum / (double) count;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getQueryCount(QueryResolvedBy queryResolvedBy) {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long first = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        String col = "total";
        if (queryResolvedBy != null) {
            col = switch (queryResolvedBy) {
                case CACHE -> "cache";
                case BLOCKED -> "blocked";
                case LOCAL -> "local";
                case UPSTREAM -> "upstream";
                case REFUSED -> "refused";
                case SERVFAIL -> "servfail";
            };
        }

        String sql = "SELECT SUM(" + col + ") FROM bucket_agg WHERE bucket_start_ms >= ? AND bucket_start_ms <= ?";
        try (PreparedStatement ps = readConn.prepareStatement(sql)) {
            ps.setLong(1, first);
            ps.setLong(2, latestAllowedBucket);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private QueryEvent readQueryEventRow(ResultSet rs) throws SQLException {
        long time = rs.getLong(1);
        String resolvedByStr = rs.getString(2);
        QueryResolvedBy resolvedBy = (resolvedByStr == null) ? null : QueryResolvedBy.valueOf(resolvedByStr);

        QueryEvent ev = new QueryEvent(time, rs.getLong(8));
        ev.setQueryResolvedBy(resolvedBy);
        ev.setClient(rs.getString(3));
        ev.setType(rs.getString(4));
        ev.setDomain(rs.getString(5));
        ev.setAnswer(rs.getString(6));
        ev.setSource(rs.getString(7));
        return ev;
    }

    private void flushOnceSafe() {
        try {
            flushOnce();
        } catch (Exception e) {
            log.warn("stats db flush failed", e);
        }
    }

    public void flushOnce() throws SQLException {
        if (closed) {
            return;
        }

        List<QueryEvent> drained = new ArrayList<>(Math.min(queue.size(), 10_000));
        queue.drainTo(drained);
        if (drained.isEmpty()) {
            applyRetention(writerConn);
            return;
        }

        writerConn.setAutoCommit(false);
        try {
            writeBatch(writerConn, drained);
            applyRetention(writerConn);
            writerConn.commit();
        } catch (SQLException e) {
            writerConn.rollback();
            throw e;
        } finally {
            writerConn.setAutoCommit(true);
        }
    }

    private void writeBatch(Connection conn, List<QueryEvent> events) throws SQLException {
        String insertEvent = "INSERT INTO query_event(time_ms, resolved_by, client, type, domain, answer, source, elapsed_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String upsertBucket = "INSERT INTO bucket_agg(bucket_start_ms, total, cache, blocked, local, upstream, refused, servfail, elapsed_sum) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(bucket_start_ms) DO UPDATE SET " +
                "total = total + excluded.total, " +
                "cache = cache + excluded.cache, " +
                "blocked = blocked + excluded.blocked, " +
                "local = local + excluded.local, " +
                "upstream = upstream + excluded.upstream, " +
                "refused = refused + excluded.refused, " +
                "servfail = servfail + excluded.servfail, " +
                "elapsed_sum = elapsed_sum + excluded.elapsed_sum";

        String upsertUpstream = "INSERT INTO upstream_hit(bucket_start_ms, upstream, hits) VALUES(?, ?, ?) " +
                "ON CONFLICT(bucket_start_ms, upstream) DO UPDATE SET hits = hits + excluded.hits";

        try (PreparedStatement ins = conn.prepareStatement(insertEvent);
             PreparedStatement bucket = conn.prepareStatement(upsertBucket);
             PreparedStatement upstream = conn.prepareStatement(upsertUpstream)) {

            boolean hasQueryEvent = false;
            for (QueryEvent e : events) {
                if (!e.isAnonymized()) {
                    hasQueryEvent = true;
                    ins.setLong(1, e.getTime());
                    ins.setString(2, e.getQueryResolvedBy() == null ? null : e.getQueryResolvedBy().name());
                    ins.setString(3, e.getClient());
                    ins.setString(4, e.getType());
                    ins.setString(5, e.getDomain());
                    ins.setString(6, e.getAnswer());
                    ins.setString(7, e.getSource());
                    ins.setLong(8, e.getElapsedTime());
                    ins.addBatch();
                }

                long bucketStart = truncateToWindow(e.getTime(), bucketIntervalMs);

                long cache = 0, blocked = 0, local = 0, up = 0, refused = 0, servfail = 0;
                QueryResolvedBy rb = e.getQueryResolvedBy();
                if (rb != null) {
                    switch (rb) {
                        case CACHE -> cache = 1;
                        case BLOCKED -> blocked = 1;
                        case LOCAL -> local = 1;
                        case UPSTREAM -> up = 1;
                        case REFUSED -> refused = 1;
                        case SERVFAIL -> servfail = 1;
                    }
                }

                bucket.setLong(1, bucketStart);
                bucket.setLong(2, 1);
                bucket.setLong(3, cache);
                bucket.setLong(4, blocked);
                bucket.setLong(5, local);
                bucket.setLong(6, up);
                bucket.setLong(7, refused);
                bucket.setLong(8, servfail);
                bucket.setLong(9, e.getElapsedTime());
                bucket.addBatch();

                if (rb == QueryResolvedBy.UPSTREAM && e.getSource() != null) {
                    upstream.setLong(1, bucketStart);
                    upstream.setString(2, e.getSource());
                    upstream.setLong(3, 1);
                    upstream.addBatch();
                }
            }

            if (hasQueryEvent) {
                ins.executeBatch();
            }
            bucket.executeBatch();
            upstream.executeBatch();
        }
    }

    private void applyRetention(Connection conn) throws SQLException {
        long now = nowSupplier.getAsLong();
        long nowBucket = truncateToWindow(now, bucketIntervalMs);
        long latestAllowedBucket = Math.max(nowBucket, maxBucketStartPresent());
        long earliestAllowed = latestAllowedBucket - (long) (maxBuckets - 1) * bucketIntervalMs;
        long cutoffTime = earliestAllowed;

        try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM query_event WHERE time_ms < ?")) {
            ps1.setLong(1, cutoffTime);
            ps1.executeUpdate();
        }
        try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM bucket_agg WHERE bucket_start_ms < ?")) {
            ps2.setLong(1, earliestAllowed);
            ps2.executeUpdate();
        }
        try (PreparedStatement ps3 = conn.prepareStatement("DELETE FROM upstream_hit WHERE bucket_start_ms < ?")) {
            ps3.setLong(1, earliestAllowed);
            ps3.executeUpdate();
        }
    }

    public static long truncateToWindow(long epochMs, long intervalMs) {
        return (epochMs / intervalMs) * intervalMs;
    }

    private long maxBucketStartPresent() {
        String sql = "SELECT MAX(bucket_start_ms) FROM bucket_agg";
        try (Statement st = readConn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            long v = rs.getLong(1);
            if (rs.wasNull()) {
                return Long.MIN_VALUE;
            }
            return v;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        writerExecutor.shutdown();
        try {
            writerExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // best-effort final flush (writer already stopped)
            flushOnceSafe();
        } catch (Exception ignored) {
        }

        closed = true;

        try {
            if (writerConn != null) {
                writerConn.close();
            }
        } catch (SQLException ignored) {
        }
        try {
            if (readConn != null) {
                readConn.close();
            }
        } catch (SQLException ignored) {
        }
    }

    // --- Test helpers (package-private) ---
    long pendingQueueSizeForTests() {
        return queue.size();
    }

    long dbPathHashForTests() {
        return dbPath.toString().hashCode();
    }
}
