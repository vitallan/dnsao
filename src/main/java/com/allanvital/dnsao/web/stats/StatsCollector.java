package com.allanvital.dnsao.web.stats;

import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;

import java.util.List;
import java.util.Map;

public interface StatsCollector {

    Map<String, Long> getUpstreamIndividualHits();
    Map<Long, Bucket> getBucketsFilledAnchoredToNow();
    List<QueryEvent> getOrderedQueryEvents();
    List<QueryEvent> getOrderedQueryEvents(int page);
    PagedQueryResult getOrderedQueryEvents(int page, int pageSize, String filter, String sortKey, String sortDir);
    double getQueryElapsedTime();
    long getQueryCount(QueryResolvedBy queryResolvedBy);

}
