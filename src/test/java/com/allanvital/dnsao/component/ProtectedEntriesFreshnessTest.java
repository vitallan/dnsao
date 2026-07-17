package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_HIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xbill.DNS.Rcode.NOERROR;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ProtectedEntriesFreshnessTest extends TestHolder {

    private static final String KEEP_DOMAIN = "url-to-keep1.com";
    private static final String HOT_DOMAIN = "hot-threshold.com";
    private static final String COLD_DOMAIN = "cold-threshold.com";

    @Test
    public void explicitKeepEntryShouldStillBeServedAsCacheHitWhenRefreshResultIsNonWarmable() throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        safeStart("1udp-cache-keep.yml");
        CacheManager cacheManager = assembler.getCacheManager();

        String key = cacheKey(Type.AAAA, KEEP_DOMAIN);
        Message cachedResponse = buildNoErrorEmptyResponse(KEEP_DOMAIN, Type.AAAA);
        cacheManager.put(key, cachedResponse, 1L);

        assertNotNull(cacheManager.safeGet(key));
        testTimeProvider.walkNow(1500L);
        Thread.sleep(1800L);

        Message response = executeCustomRequest(buildRequest(KEEP_DOMAIN, Type.AAAA));

        assertNotNull(response);
        assertEquals(NOERROR, response.getRcode());
        eventListener.assertCount(CACHE_HIT, 1, false);
    }

    @Test
    public void topProtectedEntryShouldStillBeServedAsCacheHitWhenRefreshResultIsNonWarmable() throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        loadConf("cache/1udp-cache-rewarm-threshold-top1.yml");
        conf.getMisc().setQueryLog(false);
        startFakeServer();
        safeStartWithPresetConf(true);
        CacheManager cacheManager = assembler.getCacheManager();

        String hotKey = cacheKey(Type.A, HOT_DOMAIN);
        String coldKey = cacheKey(Type.A, COLD_DOMAIN);
        Message hotCachedResponse = buildNoErrorEmptyResponse(HOT_DOMAIN, Type.A);
        Message coldCachedResponse = buildNoErrorEmptyResponse(COLD_DOMAIN, Type.A);

        cacheManager.put(hotKey, hotCachedResponse, 1L);
        cacheManager.put(coldKey, coldCachedResponse, 1L);
        cacheManager.get(hotKey);
        cacheManager.get(coldKey);
        cacheManager.get(hotKey);

        assertNotNull(cacheManager.safeGet(hotKey));
        testTimeProvider.walkNow(1500L);
        Thread.sleep(1800L);

        Message response = executeCustomRequest(buildRequest(HOT_DOMAIN, Type.A));

        assertNotNull(response);
        assertEquals(NOERROR, response.getRcode());
        eventListener.assertCount(CACHE_HIT, 3, false);
    }

    private Message buildNoErrorEmptyResponse(String domain, int type) {
        Message query = buildRequest(domain, type);
        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        return response;
    }

    private Message buildRequest(String domain, int type) {
        try {
            String qname = domain.endsWith(".") ? domain : domain + ".";
            return Message.newQuery(Record.newRecord(Name.fromString(qname), type, DClass.IN));
        } catch (Exception e) {
            throw new IllegalStateException("failed to build request", e);
        }
    }

    private String cacheKey(int type, String domain) {
        String qname = domain.endsWith(".") ? domain : domain + ".";
        return Type.string(type) + ":" + qname;
    }

    private Message executeCustomRequest(Message request) throws IOException {
        SimpleResolver resolver = new SimpleResolver(LOCAL);
        resolver.setPort(dnsServer.getUdpPort());
        resolver.setTCP(false);
        return resolver.send(request);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }
}
