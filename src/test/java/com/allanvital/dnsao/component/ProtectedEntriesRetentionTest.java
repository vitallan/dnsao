package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ProtectedEntriesRetentionTest extends TestHolder {

    private static final String KEEP_DOMAIN = "url-to-keep1.com";
    private static final String HOT_DOMAIN = "hot-threshold.com";
    private static final String COLD_DOMAIN = "cold-threshold.com";

    @Test
    public void explicitKeepEntryShouldRemainInCacheEvenWhenRewarmResultIsNonWarmable() throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        safeStart("1udp-cache-keep.yml");
        CacheManager cacheManager = assembler.getCacheManager();

        String key = key(Type.AAAA, KEEP_DOMAIN);
        Message nonWarmableResponse = buildNoErrorEmptyResponse(KEEP_DOMAIN, Type.AAAA);
        cacheManager.put(key, nonWarmableResponse, 1L);

        assertTrue(cacheManager.shouldAlwaysRewarm(key, nonWarmableResponse.getQuestion()));

        testTimeProvider.walkNow(1500, TimeUnit.MILLISECONDS);
        waitForBackgroundProcessing();

        DnsCacheEntry entry = cacheManager.safeGet(key);
        assertNotNull(entry, "explicit keep entry should still be retained in cache");
    }

    @Test
    public void topProtectedEntryShouldRemainInCacheEvenWhenRewarmResultIsNonWarmable() throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        loadConf("cache/1udp-cache-rewarm-threshold-top1.yml");
        conf.getMisc().setQueryLog(false);
        startFakeServer();
        prepareSimpleMockResponse(HOT_DOMAIN, "10.10.10.11", 1);
        prepareSimpleMockResponse(COLD_DOMAIN, "10.10.10.12", 1);
        safeStartWithPresetConf(true);

        CacheManager cacheManager = assembler.getCacheManager();
        executeRequestOnOwnServer(HOT_DOMAIN);
        executeRequestOnOwnServer(COLD_DOMAIN);
        executeRequestOnOwnServer(HOT_DOMAIN);

        String key = key(Type.A, HOT_DOMAIN);
        Message nonWarmableResponse = buildNoErrorEmptyResponse(HOT_DOMAIN, Type.A);
        cacheManager.put(key, nonWarmableResponse, 1L);

        assertTrue(cacheManager.shouldAlwaysRewarm(key, nonWarmableResponse.getQuestion()));

        testTimeProvider.walkNow(1500, TimeUnit.MILLISECONDS);
        waitForBackgroundProcessing();

        DnsCacheEntry entry = cacheManager.safeGet(key);
        assertNotNull(entry, "top protected entry should still be retained in cache");
    }

    private Message buildNoErrorEmptyResponse(String domain, int type) {
        Message query = type == Type.A ? MessageHelper.buildARequest(domain) : Message.newQuery(org.xbill.DNS.Record.newRecord(questionName(domain), type, org.xbill.DNS.DClass.IN));
        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        return response;
    }

    private org.xbill.DNS.Name questionName(String domain) {
        try {
            return org.xbill.DNS.Name.fromString(normalizeDomain(domain));
        } catch (Exception e) {
            throw new IllegalStateException("failed to build question name", e);
        }
    }

    private String key(int type, String domain) {
        return org.xbill.DNS.Type.string(type) + ":" + normalizeDomain(domain);
    }

    private String normalizeDomain(String domain) {
        if (domain.endsWith(".")) {
            return domain;
        }
        return domain + ".";
    }

    private void waitForBackgroundProcessing() throws InterruptedException {
        Thread.sleep(1800L);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }
}
