package com.allanvital.dnsao.web.json;

import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.web.StatsCollector;
import com.allanvital.dnsao.web.pojo.Bucket;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import static com.allanvital.dnsao.notification.QueryResolvedBy.*;
import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

public class JsonBuilder {

    private final StatsCollector statsCollector;

    public JsonBuilder(StatsCollector statsCollector) {
        this.statsCollector = statsCollector;
    }

    public JsonObject buildHomeJsonStats() {
        JsonObject root = Json.object();
        addSummarization(root);
        addPerUpstreamCount(root);
        addQueriesPerBucket(root);
        return root;
    }

    public JsonObject buildQueriesArray() {
        JsonObject root = Json.object();
        addOrderedQueries(root);
        return root;
    }

    private void addOrderedQueries(JsonObject root) {
        JsonArray rows = Json.array();
        for (QueryEvent q : statsCollector.getOrderedQueryEvents()) {
            JsonArray row = Json.array();
            row.add(formatMillis(q.getTime(), "HH:mm:ss.SSS"));
            row.add(q.getQueryResolvedBy().toString());
            row.add(q.getClient());
            row.add(q.getType());
            row.add(q.getDomain());
            row.add(q.getAnswer());
            row.add(q.getSource());
            row.add(q.getElapsedTime());
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
        NavigableMap<Long, Bucket> countsFilledAnchoredToNow = statsCollector.getBucketsFilledAnchoredToNow();

        for (Map.Entry<Long, Bucket> entry : countsFilledAnchoredToNow.entrySet()) {
            JsonArray cells = Json.array();
            Bucket bucket = entry.getValue();
            cells.add(formatMillis(entry.getKey(), "HH:mm")).add(bucket.getTotalCounter()).add(bucket.getCounter(CACHE))
                            .add(bucket.getCounter(BLOCKED)).add(bucket.getCounter(LOCAL))
                            .add(bucket.getCounter(UPSTREAM)).add(bucket.getCounter(REFUSED))
                            .add(bucket.getCounter(SERVFAIL));
            rows.add(cells);
        }

        JsonObject inner = Json.object();
        inner.add("columns", columns);
        inner.add("rows", rows);
        root.add("temporal", inner);
    }

    private void addSummarization(JsonObject root) {
        JsonObject summary = Json.object();

        summary.add("total", statsCollector.getQueryCount());
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
        ConcurrentHashMap<String, LongAdder> upstreamIndividualHits = statsCollector.getUpstreamIndividualHits();
        for (Map.Entry<String, LongAdder> entry : upstreamIndividualHits.entrySet()) {
            upstream.add(entry.getKey(), entry.getValue().sum());
        }
        root.add("upstream", upstream);
    }

}
