package com.allanvital.dnsao.web.stats.db;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.Bucket;
import com.allanvital.dnsao.web.stats.StatsCollector;

import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DbStatsCollector implements StatsCollector {

    @Override
    public Map<String, Long> getUpstreamIndividualHits() {
        return Map.of();
    }

    @Override
    public Map<Long, Bucket> getBucketsFilledAnchoredToNow() {
        return Map.of();
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents() {
        return List.of();
    }

    @Override
    public List<QueryEvent> getOrderedQueryEvents(int page) {
        return List.of();
    }

    @Override
    public double getQueryElapsedTime() {
        return 0;
    }

    @Override
    public long getQueryCount(QueryResolvedBy queryResolvedBy) {
        return 0;
    }

}
