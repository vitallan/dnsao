package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.dns.remote.UpstreamRoutingPolicy;
import org.xbill.DNS.Record;

public record RewarmContext(
        String key,
        RewarmTask task,
        DnsCacheEntry entry,
        Record question,
        UpstreamRoutingPolicy upstreamRoutingPolicy,
        int currentRewarmCount,
        boolean shouldAlwaysRewarm
) {
    public RewarmContext(String key,
                         RewarmTask task,
                         DnsCacheEntry entry,
                         Record question,
                         int currentRewarmCount,
                         boolean shouldAlwaysRewarm) {
        this(key, task, entry, question, null, currentRewarmCount, shouldAlwaysRewarm);
    }
}
