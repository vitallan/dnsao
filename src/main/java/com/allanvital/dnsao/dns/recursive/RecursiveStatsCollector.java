package com.allanvital.dnsao.dns.recursive;

import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface RecursiveStatsCollector {

    void increment(RecursiveMetric recursiveMetric);

    void add(RecursiveMetric recursiveMetric, long delta);

    Map<Long, RecursiveStatsBucket> getBucketsFilledAnchoredToNow();

    long getCount(RecursiveMetric recursiveMetric);

}
