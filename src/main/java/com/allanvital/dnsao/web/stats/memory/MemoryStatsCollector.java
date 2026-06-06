package com.allanvital.dnsao.web.stats.memory;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryEventListener;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.Bucket;
import com.allanvital.dnsao.web.stats.StatsCollector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.allanvital.dnsao.Constants.STATS_WINDOW_MS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MemoryStatsCollector implements QueryEventListener, StatsCollector {


    //10 minutes window
    public static final long DEFAULT_BUCKET_INTERVAL_MS = 10 * 60_000L;
    // 24 hour retention
    public static final long DEFAULT_WINDOW_MS = STATS_WINDOW_MS;
    public static final int DEFAULT_PAGE_SIZE = 25;

    private final long bucketIntervalMs;
    private final int maxBuckets;
    private final int pageSize;

    private final ConcurrentSkipListMap<Long, MemoryBucket> buckets = new ConcurrentSkipListMap<>();

    private final LongSupplier nowSupplier;

    private final ConcurrentSkipListSet<String> KNOWN_UPSTREAMS = new ConcurrentSkipListSet<>();

    public MemoryStatsCollector() {
        this(DEFAULT_BUCKET_INTERVAL_MS, DEFAULT_WINDOW_MS, DEFAULT_PAGE_SIZE, Clock::currentTimeInMillis);
    }

    public MemoryStatsCollector(long bucketIntervalMs, long windowMs, LongSupplier nowSupplier) {
        this(bucketIntervalMs, windowMs, DEFAULT_PAGE_SIZE, nowSupplier);
    }

    public MemoryStatsCollector(long bucketIntervalMs, long windowMs, int pageSize, LongSupplier nowSupplier) {
        this.bucketIntervalMs = bucketIntervalMs;
        this.nowSupplier = nowSupplier;
        this.pageSize = pageSize;
        this.maxBuckets = Math.max(1, (int) (windowMs / bucketIntervalMs));
    }

    @Override
    public void receiveNewQuery(QueryEvent queryEvent) {
        Log.DNS.debug("{}", queryEvent);

        long bucketStart = truncateToWindow(queryEvent.getTime(), bucketIntervalMs);
        MemoryBucket memoryBucket = buckets.computeIfAbsent(bucketStart, k -> new MemoryBucket());
        memoryBucket.increment(queryEvent);
        trimOldBucketsByLatest(bucketStart);

        QueryResolvedBy queryResolvedBy = queryEvent.getQueryResolvedBy();
        if (queryResolvedBy == null) {
            return;
        }

        if (queryResolvedBy == QueryResolvedBy.UPSTREAM) {
            String source = queryEvent.getSource();
            if (source == null) {
                return;
            }
            KNOWN_UPSTREAMS.add(source);
        }
    }

    @Override
    public long getQueryCount(QueryResolvedBy queryResolvedBy) {
        maybeTrimByNow();
        NavigableMap<Long, MemoryBucket> countsRaw = getMemoryBucketsFilledToNow();
        AtomicLong count = new AtomicLong();
        countsRaw.forEach((interval, memoryBucket) -> {
            count.set(count.get() + memoryBucket.getCounter(queryResolvedBy));
        });
        return count.get();
    }

    @Override
    public Map<String, Long> getUpstreamIndividualHits() {
        maybeTrimByNow();
        ConcurrentHashMap<String, Long> toReturn = new ConcurrentHashMap<>();
        for (String upstream : KNOWN_UPSTREAMS) {
            toReturn.put(upstream, 0L);
        }
        NavigableMap<Long, MemoryBucket> countsRaw = getMemoryBucketsFilledToNow();
        countsRaw.forEach((interval, memoryBucket) -> {
            Map<String, LongAdder> upstreamHits = memoryBucket.getUpstreamHits();
            for (Map.Entry<String, LongAdder> entry : upstreamHits.entrySet()) {
                String upstream = entry.getKey();
                LongAdder hits = entry.getValue();
                if (toReturn.containsKey(upstream)) {
                    Long counted = toReturn.get(upstream);
                    counted = counted + hits.sum();
                    toReturn.put(upstream, counted);
                } else {
                    toReturn.put(upstream, hits.sum());
                }
            }
        });
        return toReturn;
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents() {
        maybeTrimByNow();
        LinkedList<QueryEvent> queryEvents = new LinkedList<>();
        NavigableMap<Long, MemoryBucket> countsRaw = getMemoryBucketsFilledToNow();
        countsRaw.forEach((interval, memoryBucket) -> {
            queryEvents.addAll(memoryBucket.getQueryEvents());
        });
        queryEvents.sort(Comparator.comparingLong(QueryEvent::getTime).reversed()); //argh
        return queryEvents;
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents(int page) {
        maybeTrimByNow();
        LinkedList<QueryEvent> queryEvents = new LinkedList<>();
        NavigableSet<Long> descendingKeySet = buckets.descendingKeySet();
        int starterIndex = page * pageSize;
        int finalIndex = starterIndex + pageSize;
        int currentIndex = 0;
        for (Long key : descendingKeySet) {
            MemoryBucket memoryBucket = buckets.get(key);
            PriorityBlockingQueue<QueryEvent> bucketQueryEvents = memoryBucket.getQueryEvents();
            int size = bucketQueryEvents.size();
            if (size == 0) {
                continue;
            }
            List<QueryEvent> list = bucketQueryEvents.stream().toList();
            for (int i = size - 1; i >= 0; i--) {
                QueryEvent queryEvent = list.get(i);
                if (currentIndex >= starterIndex && currentIndex < finalIndex) {
                    queryEvents.add(queryEvent);
                }
                currentIndex++;
            }
            if (currentIndex >= finalIndex) {
                break;
            }
        }
        return queryEvents;
    }

    @Override
    public double getQueryElapsedTime() {
        long count = getQueryCount(null);
        if (count == 0) {
            return 0.0;
        }
        NavigableMap<Long, MemoryBucket> countsRaw = getMemoryBucketsFilledToNow();
        AtomicLong elapseTimeSum = new AtomicLong();
        countsRaw.forEach((interval, memoryBucket) -> {
            elapseTimeSum.set(elapseTimeSum.get() + memoryBucket.getElapseTimeSum());
        });
        return elapseTimeSum.get() / (double) count;
    }

    public NavigableMap<Long, Long> getCountsFilledAnchoredToNow() {
        return buildAnchoredToNow(MemoryBucket::getTotalCounter, () -> 0L);
    }

    @Override
    public Map<Long, Bucket> getBucketsFilledAnchoredToNow() {
        return new HashMap<>(getMemoryBucketsFilledToNow());
    }

    public NavigableMap<Long, MemoryBucket> getMemoryBucketsFilledToNow() {
        return buildAnchoredToNow(Function.identity(), MemoryBucket::new);
    }

    public NavigableMap<Long, Long> getCountsRaw() {
        return transformRaw(MemoryBucket::getTotalCounter);
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

    private <T> NavigableMap<Long, T> buildAnchoredToNow(Function<MemoryBucket, T> mapper,
                                                         Supplier<T> defaultSupplier) {
        maybeTrimByNow();

        NavigableMap<Long, T> out = new TreeMap<>();
        long nowBucket = truncateToWindow(nowSupplier.getAsLong(), bucketIntervalMs);
        long first = nowBucket - (long) (maxBuckets - 1) * bucketIntervalMs;

        for (long t = first; t <= nowBucket; t += bucketIntervalMs) {
            MemoryBucket b = buckets.get(t);
            out.put(t, (b == null) ? defaultSupplier.get() : mapper.apply(b));
        }
        return out;
    }

    private <T> NavigableMap<Long, T> transformRaw(Function<MemoryBucket, T> mapper) {
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