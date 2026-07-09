package com.allanvital.dnsao.cache.map;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.Constants;
import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.cache.SizeSnapshot;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Record;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Consumer;


public class KeepAwareLruDnsCache extends LinkedHashMap<String, DnsCacheEntry> implements CacheStats {

    private final int maxSize;
    private final KeepProvider keepProvider;
    private final long bucketIntervalMs;
    private final int maxHistoryEntries;
    private final ConcurrentLinkedDeque<Long> evictionTimes = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<SizeSnapshot> sizeHistory = new ConcurrentLinkedDeque<>();
    private final ScheduledExecutorService sizeSampler;
    private final LongSupplier nowSupplier;
    private final Consumer<String> evictionListener;
    private boolean warnedAllKeepOverCapacity;

    public KeepAwareLruDnsCache(int maxSize, KeepProvider keepProvider) {
        this(maxSize, keepProvider, Constants.STATS_BUCKET_INTERVAL_MS, Constants.STATS_WINDOW_MS, Clock::currentTimeInMillis, null);
    }

    public KeepAwareLruDnsCache(int maxSize, KeepProvider keepProvider, long bucketIntervalMs, long windowMs, LongSupplier nowSupplier) {
        this(maxSize, keepProvider, bucketIntervalMs, windowMs, nowSupplier, null);
    }

    public KeepAwareLruDnsCache(int maxSize,
                                KeepProvider keepProvider,
                                long bucketIntervalMs,
                                long windowMs,
                                LongSupplier nowSupplier,
                                Consumer<String> evictionListener) {
        super(Math.max(16, maxSize), 0.75f, true);
        this.maxSize = maxSize;
        this.keepProvider = keepProvider;
        this.bucketIntervalMs = bucketIntervalMs;
        this.maxHistoryEntries = (int) (windowMs / bucketIntervalMs);
        this.nowSupplier = nowSupplier;
        this.evictionListener = evictionListener;
        this.sizeSampler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-size-sampler");
            t.setDaemon(true);
            return t;
        });
        recordSizeSnapshot();
        sizeSampler.scheduleAtFixedRate(this::recordSizeSnapshot, bucketIntervalMs, bucketIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public DnsCacheEntry put(String key, DnsCacheEntry value) {
        DnsCacheEntry prev = super.put(key, value);
        trimIfNeeded();
        return prev;
    }

    private void trimIfNeeded() {
        if (maxSize <= 0) {
            return;
        }
        while (size() > maxSize) {
            boolean removed = removeEldestNonKeep();
            if (removed) {
                continue;
            }

            if (!warnedAllKeepOverCapacity) {
                warnedAllKeepOverCapacity = true;
                Log.CACHE.warn("cache over capacity (size={}, max={}) but all entries are in keep; allowing size to exceed maxCacheEntries", size(), maxSize);
            }
            break;
        }
    }

    private boolean removeEldestNonKeep() {
        Iterator<Map.Entry<String, DnsCacheEntry>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DnsCacheEntry> e = it.next();
            if (e == null) {
                continue;
            }
            DnsCacheEntry entry = e.getValue();
            if (!isKeep(entry)) {
                String key = e.getKey();
                it.remove();
                evictionTimes.addLast(Clock.currentTimeInMillis());
                if (evictionListener != null) {
                    evictionListener.accept(key);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int getCurrentSize() {
        return size();
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public long getEvictionCount() {
        long now = Clock.currentTimeInMillis();
        long cutoff = now - Constants.STATS_WINDOW_MS;
        while (!evictionTimes.isEmpty() && evictionTimes.peekFirst() < cutoff) {
            evictionTimes.pollFirst();
        }
        return evictionTimes.size();
    }

    @Override
    public List<SizeSnapshot> getSizeHistory() {
        recordSizeSnapshot();
        long now = nowSupplier.getAsLong();
        long currentBucket = (now / bucketIntervalMs) * bucketIntervalMs;
        long cutoff = currentBucket - (maxHistoryEntries - 1) * bucketIntervalMs;
        List<SizeSnapshot> result = new ArrayList<>(maxHistoryEntries);
        for (int i = 0; i < maxHistoryEntries; i++) {
            long ts = cutoff + i * bucketIntervalMs;
            int size = findLatestSizeAtOrBefore(ts);
            result.add(new SizeSnapshot(ts, size));
        }
        return result;
    }

    private int findLatestSizeAtOrBefore(long ts) {
        Iterator<SizeSnapshot> it = sizeHistory.descendingIterator();
        while (it.hasNext()) {
            SizeSnapshot s = it.next();
            if (s.timestamp() <= ts) return s.size();
        }
        return 0;
    }

    void recordSizeSnapshot() {
        long now = nowSupplier.getAsLong();
        long bucketStart = (now / bucketIntervalMs) * bucketIntervalMs;
        sizeHistory.addLast(new SizeSnapshot(bucketStart, size()));
        long cutoff = now - bucketIntervalMs * maxHistoryEntries;
        while (!sizeHistory.isEmpty() && sizeHistory.peekFirst().timestamp() < cutoff) {
            sizeHistory.pollFirst();
        }
    }

    private boolean isKeep(DnsCacheEntry entry) {
        if (keepProvider == null || entry == null) {
            return false;
        }
        try {
            Record q = entry.getResponse() != null ? entry.getResponse().getQuestion() : null;
            return q != null && keepProvider.contain(q);
        } catch (Throwable t) {
            return false;
        }
    }
}
