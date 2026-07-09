package com.allanvital.dnsao.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class AlwaysRewarmTopEntriesTracker {

    private final int threshold;
    private final AtomicLong sequence = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> sequenceByKey = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, String> keyBySequence = new ConcurrentSkipListMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public AlwaysRewarmTopEntriesTracker(int threshold) {
        this.threshold = Math.max(0, threshold);
    }

    public long recordAccess(String key) {
        if (threshold <= 0 || key == null) {
            return 0L;
        }
        long nextSequence = sequence.incrementAndGet();
        lock.lock();
        try {
            Long oldSequence = sequenceByKey.put(key, nextSequence);
            if (oldSequence != null) {
                keyBySequence.remove(oldSequence, key);
            }
            keyBySequence.put(nextSequence, key);
            while (sequenceByKey.size() > threshold) {
                Map.Entry<Long, String> eldest = keyBySequence.pollFirstEntry();
                if (eldest == null) {
                    break;
                }
                sequenceByKey.remove(eldest.getValue(), eldest.getKey());
            }
            return nextSequence;
        } finally {
            lock.unlock();
        }
    }

    public void remove(String key) {
        if (threshold <= 0 || key == null) {
            return;
        }
        lock.lock();
        try {
            Long oldSequence = sequenceByKey.remove(key);
            if (oldSequence != null) {
                keyBySequence.remove(oldSequence, key);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isProtected(String key) {
        return threshold > 0 && key != null && sequenceByKey.containsKey(key);
    }

    public List<String> snapshotProtectedKeys() {
        lock.lock();
        try {
            return List.copyOf(new ArrayList<>(keyBySequence.descendingMap().values()));
        } finally {
            lock.unlock();
        }
    }
}
