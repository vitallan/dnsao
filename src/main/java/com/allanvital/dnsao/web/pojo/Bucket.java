package com.allanvital.dnsao.web.pojo;

import com.allanvital.dnsao.notification.QueryEvent;
import com.allanvital.dnsao.notification.QueryResolvedBy;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Bucket {

    private final LongAdder totalCounter = new LongAdder();

    private final LongAdder blocked = new LongAdder();
    private final LongAdder cached = new LongAdder();
    private final LongAdder upstream = new LongAdder();
    private final LongAdder local = new LongAdder();
    private final LongAdder refused = new LongAdder();
    private final LongAdder servfail = new LongAdder();

    private final ConcurrentSkipListMap<String, LongAdder> upstreamsHit = new ConcurrentSkipListMap<>();

    public void increment(QueryEvent queryEvent) {
        totalCounter.increment();
        if (queryEvent.getQueryResolvedBy() != null) {
            switch (queryEvent.getQueryResolvedBy()) {
                case CACHE -> cached.increment();
                case BLOCKED -> blocked.increment();
                case LOCAL -> local.increment();
                case REFUSED -> refused.increment();
                case SERVFAIL -> servfail.increment();
                case UPSTREAM -> {
                    upstream.increment();
                    String upstream = queryEvent.getSource();
                    if (upstream != null) {
                        LongAdder longAdder = upstreamsHit.computeIfAbsent(upstream, key -> new LongAdder());
                        longAdder.increment();
                    }
                }
            }
        }
    }

    public Map<String, LongAdder> getUpstreamHits() {
        return this.upstreamsHit.clone();
    }

    public long getCounter(QueryResolvedBy queryResolvedBy) {
        long count = 0;
        switch (queryResolvedBy) {
            case CACHE -> count = cached.sum();
            case BLOCKED -> count = blocked.sum();
            case UPSTREAM -> count = upstream.sum();
            case LOCAL -> count = local.sum();
            case REFUSED -> count = refused.sum();
            case SERVFAIL -> count = servfail.sum();
        }
        return count;
    }

    public long getTotalCounter()  {
        return totalCounter.sum();
    }

}