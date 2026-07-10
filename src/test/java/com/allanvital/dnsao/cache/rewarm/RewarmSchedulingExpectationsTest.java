package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RewarmSchedulingExpectationsTest {

    @Test
    void evictedEntriesShouldAlsoCancelTheirScheduledRewarmTask() {
        CacheConf cacheConf = new CacheConf();
        cacheConf.setEnabled(true);
        cacheConf.setMaxCacheEntries(1);

        FixedTimeRewarmScheduler scheduler = new FixedTimeRewarmScheduler(100);
        CacheManager cacheManager = new CacheManager(
                cacheConf,
                scheduler,
                new ExpiredConf(),
                new KeepProvider(cacheConf)
        );

        Message first = MessageHelper.buildAResponse(MessageHelper.buildARequest("first.example"), "10.0.0.1", 30);
        Message second = MessageHelper.buildAResponse(MessageHelper.buildARequest("second.example"), "10.0.0.2", 30);

        cacheManager.put("A:first.example", first, 30L);
        cacheManager.put("A:second.example", second, 30L);

        assertNull(cacheManager.safeGet("A:first.example"));
        assertEquals(List.of("A:second.example"), scheduler.queue().stream().map(RewarmTask::getKey).toList());
        assertEquals(1, scheduler.queue().size());
    }
}
