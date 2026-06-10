package com.allanvital.dnsao.web.json;

import com.allanvital.dnsao.cache.CacheStats;
import com.allanvital.dnsao.cache.SizeSnapshot;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.web.stats.Bucket;
import com.allanvital.dnsao.web.stats.StatsCollector;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.*;
import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

public class JsonBuilder {

    private final StatsCollector statsCollector;
    private final CacheStats cacheStats;

    public JsonBuilder(StatsCollector statsCollector, CacheStats cacheStats) {
        this.statsCollector = statsCollector;
        this.cacheStats = cacheStats;
    }

    public JsonObject buildHomeJsonStats() {
        JsonObject root = Json.object();
        addSummarization(root);
        addPerUpstreamCount(root);
        addQueriesPerBucket(root);
        addCacheStats(root);
        addCacheTemporal(root);
        return root;
    }

    private void addCacheStats(JsonObject root) {
        if (cacheStats == null) {
            return;
        }
        JsonObject cache = Json.object();
        cache.add("size", cacheStats.getCurrentSize());
        cache.add("maxSize", cacheStats.getMaxSize());
        cache.add("evictionCount", cacheStats.getEvictionCount());
        root.add("cache", cache);
    }

    public JsonObject buildQueriesArray(int page) {
        JsonObject root = Json.object();
        addOrderedQueries(root, page);
        return root;
    }

    private void addOrderedQueries(JsonObject root, int page) {
        JsonArray rows = Json.array();
        for (QueryEvent queryEvent : statsCollector.getOrderedQueryEvents(page)) {
            JsonArray row = Json.array();
            row.add(formatMillis(queryEvent.getTime(), "yyyy-MM-dd HH:mm:ss.SSS"));
            row.add(queryEvent.getQueryResolvedBy().toString());
            row.add(queryEvent.getClient());
            row.add(queryEvent.getType());
            row.add(queryEvent.getDomain());
            row.add(queryEvent.getAnswer());
            row.add(queryEvent.getSource());
            row.add(queryEvent.getElapsedTime());
            rows.add(row);
        }
        root.add("queries", rows);
    }

    private void addQueriesPerBucket(JsonObject root) {
        JsonArray columns = Json.array()
                .add("ts").add("total").add("cache")
                .add("block").add("local").add("upstream")
                .add("refused").add("servfail");

        JsonArray rows = Json.array();
        Map<Long, Bucket> countsFilledAnchoredToNow = statsCollector.getBucketsFilledAnchoredToNow();

        countsFilledAnchoredToNow.keySet().stream().sorted().forEach(key -> {
            JsonArray cells = Json.array();
            Bucket bucket = countsFilledAnchoredToNow.get(key);
            cells
                    .add(formatMillis(key, "HH:mm"))
                    .add(bucket.getTotalCounter())
                    .add(bucket.getCounter(CACHE))
                    .add(bucket.getCounter(BLOCKED))
                    .add(bucket.getCounter(LOCAL))
                    .add(bucket.getCounter(UPSTREAM))
                    .add(bucket.getCounter(REFUSED))
                    .add(bucket.getCounter(SERVFAIL));
            rows.add(cells);
        });

        JsonObject inner = Json.object();
        inner.add("columns", columns);
        inner.add("rows", rows);
        root.add("temporal", inner);
    }

    private void addCacheTemporal(JsonObject root) {
        if (cacheStats == null) {
            return;
        }
        List<SizeSnapshot> history = cacheStats.getSizeHistory();
        if (history.isEmpty()) {
            return;
        }

        JsonArray columns = Json.array().add("ts").add("size");
        JsonArray rows = Json.array();

        for (SizeSnapshot snapshot : history) {
            JsonArray cells = Json.array();
            cells.add(formatMillis(snapshot.timestamp(), "HH:mm"));
            cells.add(snapshot.size());
            rows.add(cells);
        }

        JsonObject inner = Json.object();
        inner.add("columns", columns);
        inner.add("rows", rows);
        root.add("cacheTemporal", inner);
    }

    private void addSummarization(JsonObject root) {
        JsonObject summary = Json.object();

        summary.add("total", statsCollector.getQueryCount(null));
        summary.add("cache", statsCollector.getQueryCount(CACHE));
        summary.add("blocked", statsCollector.getQueryCount(BLOCKED));
        summary.add("local", statsCollector.getQueryCount(LOCAL));
        summary.add("upstream", statsCollector.getQueryCount(UPSTREAM));
        summary.add("refused", statsCollector.getQueryCount(REFUSED));
        summary.add("servfail", statsCollector.getQueryCount(SERVFAIL));
        summary.add("avgTime", statsCollector.getQueryElapsedTime());

        root.add("summary", summary);
    }

    private void addPerUpstreamCount(JsonObject root) {
        JsonObject upstream = Json.object();
        Map<String, Long> upstreamIndividualHits = statsCollector.getUpstreamIndividualHits();
        for (Map.Entry<String, Long> entry : upstreamIndividualHits.entrySet()) {
            upstream.add(entry.getKey(), entry.getValue());
        }
        root.add("upstream", upstream);
    }

}
