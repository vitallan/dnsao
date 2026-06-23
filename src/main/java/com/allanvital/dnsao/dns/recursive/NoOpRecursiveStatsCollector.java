package com.allanvital.dnsao.dns.recursive;

import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NoOpRecursiveStatsCollector implements RecursiveStatsCollector {

    @Override
    public void increment(RecursiveMetric recursiveMetric) {
    }

    @Override
    public void add(RecursiveMetric recursiveMetric, long delta) {
    }

    @Override
    public Map<Long, RecursiveStatsBucket> getBucketsFilledAnchoredToNow() {
        return Map.of();
    }

    @Override
    public long getCount(RecursiveMetric recursiveMetric) {
        return 0;
    }

}
