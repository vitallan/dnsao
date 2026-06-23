package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.infra.clock.Clock;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.LongSupplier;

import static com.allanvital.dnsao.Constants.STATS_BUCKET_INTERVAL_MS;
import static com.allanvital.dnsao.Constants.STATS_WINDOW_MS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MemoryRecursiveStatsCollector implements RecursiveStatsCollector {

    public static final long DEFAULT_BUCKET_INTERVAL_MS = STATS_BUCKET_INTERVAL_MS;
    public static final long DEFAULT_WINDOW_MS = STATS_WINDOW_MS;

    private final long bucketIntervalMs;
    private final int maxBuckets;
    private final LongSupplier nowSupplier;
    private final ConcurrentSkipListMap<Long, RecursiveStatsBucket> buckets = new ConcurrentSkipListMap<>();

    public MemoryRecursiveStatsCollector() {
        this(DEFAULT_BUCKET_INTERVAL_MS, DEFAULT_WINDOW_MS, Clock::currentTimeInMillis);
    }

    public MemoryRecursiveStatsCollector(long bucketIntervalMs, long windowMs, LongSupplier nowSupplier) {
        this.bucketIntervalMs = bucketIntervalMs;
        this.nowSupplier = nowSupplier;
        this.maxBuckets = Math.max(1, (int) (windowMs / bucketIntervalMs));
    }

    @Override
    public void increment(RecursiveMetric recursiveMetric) {
        add(recursiveMetric, 1);
    }

    @Override
    public void add(RecursiveMetric recursiveMetric, long delta) {
        long bucketStart = truncateToWindow(nowSupplier.getAsLong(), bucketIntervalMs);
        RecursiveStatsBucket recursiveStatsBucket = buckets.computeIfAbsent(bucketStart, ignored -> new RecursiveStatsBucket());
        recursiveStatsBucket.add(recursiveMetric, delta);
        trimOldBucketsByLatest(bucketStart);
    }

    @Override
    public Map<Long, RecursiveStatsBucket> getBucketsFilledAnchoredToNow() {
        maybeTrimByNow();

        NavigableMap<Long, RecursiveStatsBucket> snapshot = new TreeMap<>();
        long nowBucket = truncateToWindow(nowSupplier.getAsLong(), bucketIntervalMs);
        long first = nowBucket - (long) (maxBuckets - 1) * bucketIntervalMs;
        for (long time = first; time <= nowBucket; time += bucketIntervalMs) {
            snapshot.put(time, buckets.getOrDefault(time, new RecursiveStatsBucket()));
        }
        return snapshot;
    }

    @Override
    public long getCount(RecursiveMetric recursiveMetric) {
        maybeTrimByNow();
        long count = 0;
        for (RecursiveStatsBucket recursiveStatsBucket : buckets.values()) {
            count += recursiveStatsBucket.getCounter(recursiveMetric);
        }
        return count;
    }

    public static long truncateToWindow(long epochMs, long intervalMs) {
        return (epochMs / intervalMs) * intervalMs;
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
