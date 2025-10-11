package com.allanvital.dnsao.web;

import com.allanvital.dnsao.notification.NotificationManager;
import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.notification.QueryEventListener;
import com.allanvital.dnsao.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.pojo.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.allanvital.dnsao.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StatsCollector implements QueryEventListener {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    //5 minutes window
    public static final long DEFAULT_BUCKET_INTERVAL_MS = 10 * 60_000L;
    // 24 hour retention
    public static final long DEFAULT_WINDOW_MS = 24 * 60 * 60_000L;

    private final long bucketIntervalMs;
    private final int maxBuckets;

    private final ConcurrentSkipListMap<Long, Bucket> buckets = new ConcurrentSkipListMap<>();

    private final LongAdder totalElapsedTime = new LongAdder();
    private final LongAdder totalQueries = new LongAdder();
    private final LongAdder totalCacheHits = new LongAdder();
    private final LongAdder totalBlockHits = new LongAdder();
    private final LongAdder totalLocalHits = new LongAdder();
    private final LongAdder totalUpstreamHits = new LongAdder();
    private final LongAdder totalRefusedHits = new LongAdder();
    private final LongAdder totalServFailHits = new LongAdder();

    private final LongSupplier nowSupplier;

    private final ConcurrentSkipListSet<String> KNOWN_UPSTREAMS = new ConcurrentSkipListSet<>();

    private final ConcurrentHashMap<String, LongAdder> upstreamIndividualHits = new ConcurrentHashMap<>();

    public StatsCollector() {
        this(DEFAULT_BUCKET_INTERVAL_MS, DEFAULT_WINDOW_MS, System::currentTimeMillis);
    }

    public StatsCollector(long bucketIntervalMs, long windowMs, LongSupplier nowSupplier) {
        this.bucketIntervalMs = bucketIntervalMs;
        this.nowSupplier = nowSupplier;
        this.maxBuckets = Math.max(1, (int) (windowMs / bucketIntervalMs));
        NotificationManager.getInstance().querySubscribe(this);
    }

    @Override
    public void receiveNewQuery(QueryEvent queryEvent) {
        log.info("{}", queryEvent);

        long bucketStart = truncateToWindow(queryEvent.getTime(), bucketIntervalMs);
        Bucket bucket = buckets.computeIfAbsent(bucketStart, k -> new Bucket());
        bucket.increment(queryEvent);
        trimOldBucketsByLatest(bucketStart);

        totalQueries.increment();
        totalElapsedTime.add(queryEvent.getElapsedTime());

        QueryResolvedBy queryResolvedBy = queryEvent.getQueryResolvedBy();
        if (queryResolvedBy == null) {
            return;
        }

        LongAdder counter = getCounter(queryResolvedBy);
        if (counter != null) {
            counter.increment();
        }

        if (queryResolvedBy == QueryResolvedBy.UPSTREAM) {
            String source = queryEvent.getSource();
            if (source == null) {
                return;
            }
            KNOWN_UPSTREAMS.add(source);
            upstreamIndividualHits.computeIfAbsent(source, k -> new LongAdder());
            upstreamIndividualHits.get(source).increment();
        }
    }

    public long getQueryCount(QueryResolvedBy queryResolvedBy) {
        LongAdder counter = getCounter(queryResolvedBy);
        if (counter != null) {
            return counter.sum();
        }
        return 0;
    }

    private LongAdder getCounter(QueryResolvedBy resolvedBy) {
        return switch (resolvedBy) {
            case CACHE -> totalCacheHits;
            case BLOCKED -> totalBlockHits;
            case LOCAL -> totalLocalHits;
            case REFUSED -> totalRefusedHits;
            case SERVFAIL -> totalServFailHits;
            case UPSTREAM -> totalUpstreamHits;
        };
    }

    public ConcurrentHashMap<String, LongAdder> getUpstreamIndividualHits() {
        return upstreamIndividualHits;
    }

    public long getQueryCount() {
        return totalQueries.sum();
    }

    public double getQueryElapsedTime() {
        long count = totalQueries.sum();
        if (count == 0) {
            return 0.0;
        }
        return totalElapsedTime.sum() / (double) count;
    }

    public NavigableMap<Long, Long> getCountsFilledAnchoredToNow() {
        return buildAnchoredToNow(Bucket::getTotalCounter, () -> 0L);
    }

    public NavigableMap<Long, Bucket> getBucketsFilledAnchoredToNow() {
        return buildAnchoredToNow(Function.identity(), Bucket::new);
    }

    public NavigableMap<Long, Long> getCountsRaw() {
        return transformRaw(Bucket::getTotalCounter);
    }

    public NavigableMap<Long, Map<String, Long>> getUpstreamBucketsAnchoredToNow() {
        return buildAnchoredToNow(
                b -> toLongMap(b.getUpstreamHits()),
                this::zeroUpstreamMap
        );
    }

    public NavigableMap<Long, Map<String, Long>> getUpstreamBucketsRaw() {
        return transformRaw(b -> toLongMap(b.getUpstreamHits()));
    }

    public static long truncateToWindow(long epochMs, long intervalMs) {
        return (epochMs / intervalMs) * intervalMs;
    }

    private <T> NavigableMap<Long, T> buildAnchoredToNow(Function<Bucket, T> mapper,
                                                         Supplier<T> defaultSupplier) {
        maybeTrimByNow();

        NavigableMap<Long, T> out = new TreeMap<>();
        long nowBucket = truncateToWindow(nowSupplier.getAsLong(), bucketIntervalMs);
        long first = nowBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        for (long t = first; t <= nowBucket; t += bucketIntervalMs) {
            Bucket b = buckets.get(t);
            out.put(t, (b == null) ? defaultSupplier.get() : mapper.apply(b));
        }
        return out;
    }

    private <T> NavigableMap<Long, T> transformRaw(Function<Bucket, T> mapper) {
        maybeTrimByNow();

        NavigableMap<Long, T> out = new TreeMap<>();
        buckets.forEach((k, v) -> out.put(k, mapper.apply(v)));
        return out;
    }

    private Map<String, Long> toLongMap(Map<String, LongAdder> src) {
        Map<String, Long> m = new HashMap<>(src.size());
        src.forEach((k, v) -> m.put(k, v.sum()));
        return m;
    }

    private Map<String, Long> zeroUpstreamMap() {
        Map<String, Long> m = new HashMap<>(KNOWN_UPSTREAMS.size());
        for (String u : KNOWN_UPSTREAMS) {
            m.put(u, 0L);
        }
        return m;
    }

    private void trimOldBucketsByLatest(long latestBucketStart) {
        long earliestAllowed = latestBucketStart - (long) (maxBuckets - 1) * bucketIntervalMs;
        buckets.headMap(earliestAllowed).clear();
        while (buckets.size() > maxBuckets) {
            buckets.pollFirstEntry();
        }
    }

    private void maybeTrimByNow() {
        if (buckets.isEmpty()) {
            return;
        }
        long nowBucket = truncateToWindow(nowSupplier.getAsLong(), bucketIntervalMs);
        long earliestAllowed = nowBucket - (long) (maxBuckets - 1) * bucketIntervalMs;
        buckets.headMap(earliestAllowed).clear();
        while (buckets.size() > maxBuckets) {
            buckets.pollFirstEntry();
        }
    }

}