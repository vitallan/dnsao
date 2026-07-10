package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import org.xbill.DNS.Record;

public record RewarmContext(
        String key,
        RewarmTask task,
        DnsCacheEntry entry,
        Record question,
        int currentRewarmCount,
        boolean shouldAlwaysRewarm
) {
}
