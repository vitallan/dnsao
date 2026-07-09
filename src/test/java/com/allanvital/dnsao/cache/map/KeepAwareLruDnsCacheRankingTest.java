package com.allanvital.dnsao.cache.map;

import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeepAwareLruDnsCacheRankingTest {

    @Test
    void shouldReturnTopNonKeepEntriesByRecency() throws Exception {
        KeepAwareLruDnsCache cache = cacheWithKeep("keep-ranking.com");

        cache.put("keep", entry("keep-ranking.com"));
        cache.put("a", entry("a-ranking.com"));
        cache.put("b", entry("b-ranking.com"));
        cache.put("c", entry("c-ranking.com"));

        cache.get("a");
        cache.get("keep");
        cache.get("c");

        assertEquals(List.of("c", "a"), cache.getTopNonKeepKeys(2));
    }

    @Test
    void shouldIgnoreKeepEntriesWhenCountingThreshold() throws Exception {
        KeepAwareLruDnsCache cache = cacheWithKeep("keep-ranking.com");

        cache.put("keep", entry("keep-ranking.com"));
        cache.put("a", entry("a-ranking.com"));
        cache.put("b", entry("b-ranking.com"));

        cache.get("a");
        cache.get("keep");

        assertEquals(List.of("a"), cache.getTopNonKeepKeys(1));
    }

    @Test
    void shouldReturnAllNonKeepEntriesWhenThresholdIsLargerThanPopulation() throws Exception {
        KeepAwareLruDnsCache cache = cacheWithKeep("keep-ranking.com");

        cache.put("keep", entry("keep-ranking.com"));
        cache.put("a", entry("a-ranking.com"));
        cache.put("b", entry("b-ranking.com"));

        cache.get("b");

        assertEquals(List.of("b", "a"), cache.getTopNonKeepKeys(10));
    }

    private KeepAwareLruDnsCache cacheWithKeep(String keepDomain) {
        CacheConf cacheConf = new CacheConf();
        cacheConf.setKeep(List.of(keepDomain));
        KeepProvider keepProvider = new KeepProvider(cacheConf);
        return new KeepAwareLruDnsCache(10, keepProvider);
    }

    private DnsCacheEntry entry(String domain) {
        Message response = MessageHelper.buildAResponse(MessageHelper.buildARequest(domain), "10.10.10.10", 300);
        return new DnsCacheEntry(response, 300L);
    }
}
