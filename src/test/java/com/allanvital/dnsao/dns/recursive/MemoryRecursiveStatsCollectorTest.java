package com.allanvital.dnsao.dns.recursive;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MemoryRecursiveStatsCollectorTest {

    @Test
    public void keepsOnlyTheLastWindowOfBuckets() {
        AtomicLong now = new AtomicLong(0);
        MemoryRecursiveStatsCollector collector = new MemoryRecursiveStatsCollector(1000, 3000, now::get);

        collector.increment(RecursiveMetric.SESSION_STARTED);
        now.set(1000);
        collector.increment(RecursiveMetric.SESSION_STARTED);
        now.set(2000);
        collector.increment(RecursiveMetric.SESSION_STARTED);
        now.set(3000);
        collector.increment(RecursiveMetric.SESSION_STARTED);

        Map<Long, RecursiveStatsBucket> buckets = collector.getBucketsFilledAnchoredToNow();

        assertEquals(3, buckets.size());
        assertEquals(1, buckets.get(1000L).getCounter(RecursiveMetric.SESSION_STARTED));
        assertEquals(1, buckets.get(2000L).getCounter(RecursiveMetric.SESSION_STARTED));
        assertEquals(1, buckets.get(3000L).getCounter(RecursiveMetric.SESSION_STARTED));
        assertEquals(3, collector.getCount(RecursiveMetric.SESSION_STARTED));
    }

    @Test
    public void aggregatesCountersAcrossRetainedBuckets() {
        AtomicLong now = new AtomicLong(0);
        MemoryRecursiveStatsCollector collector = new MemoryRecursiveStatsCollector(1000, 4000, now::get);

        collector.increment(RecursiveMetric.WALK_STEP);
        collector.add(RecursiveMetric.RACE_CANDIDATE_SUM, 2);
        now.set(1000);
        collector.increment(RecursiveMetric.WALK_STEP);
        collector.add(RecursiveMetric.RACE_CANDIDATE_SUM, 3);

        assertEquals(2, collector.getCount(RecursiveMetric.WALK_STEP));
        assertEquals(5, collector.getCount(RecursiveMetric.RACE_CANDIDATE_SUM));
    }

}
