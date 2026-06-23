package com.allanvital.dnsao.dns.recursive;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveStatsBucket {

    private final EnumMap<RecursiveMetric, LongAdder> counters = new EnumMap<>(RecursiveMetric.class);

    public RecursiveStatsBucket() {
        for (RecursiveMetric recursiveMetric : RecursiveMetric.values()) {
            counters.put(recursiveMetric, new LongAdder());
        }
    }

    public void increment(RecursiveMetric recursiveMetric) {
        add(recursiveMetric, 1);
    }

    public void add(RecursiveMetric recursiveMetric, long delta) {
        if (delta <= 0) {
            return;
        }
        counters.get(recursiveMetric).add(delta);
    }

    public long getCounter(RecursiveMetric recursiveMetric) {
        return counters.get(recursiveMetric).sum();
    }

    public Map<RecursiveMetric, Long> snapshot() {
        EnumMap<RecursiveMetric, Long> snapshot = new EnumMap<>(RecursiveMetric.class);
        for (Map.Entry<RecursiveMetric, LongAdder> entry : counters.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().sum());
        }
        return snapshot;
    }

}
