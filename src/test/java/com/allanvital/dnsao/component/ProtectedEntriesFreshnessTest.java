package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.List;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_HIT;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
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

    public void setup(String scenario) throws Exception {
        registerOverride(new FixedTimeRewarmScheduler(100));
        safeStart(scenario);
        eventListener.reset();
    }

    @Test
    public void explicitKeepEntryShouldStillBeServedAsCacheHitWhenRefreshResultIsNonWarmable() throws Exception {
        setup("cache/1udp-cache-keep-single-entry.yml");

        String key = cacheKey(Type.A, KEEP_DOMAIN);
        Message cachedResponse = buildNoErrorEmptyResponseWithSoa(KEEP_DOMAIN, Type.A, 90L, 180L);
        fakeUpstreamServer.mockResponse(buildRequest(KEEP_DOMAIN, Type.A), cachedResponse);
        CacheManager cacheManager = assembler.getCacheManager();
        cacheManager.put(key, cachedResponse, 1L);

        assertNotNull(cacheManager.safeGet(key));
        eventListener.assertCount(CACHE_REWARM, 1);

        DnsCacheEntry entry = cacheManager.get(key);

        assertNotNull(entry);
        assertEquals(NOERROR, entry.getResponse().getRcode());
        eventListener.assertCount(CACHE_HIT, 1, false);
    }

    @Test
    public void topProtectedEntryShouldStillBeServedAsCacheHitWhenRefreshResultIsNonWarmable() throws Exception {
        setup("cache/1udp-cache-rewarm-threshold-top1.yml");
        CacheManager cacheManager = assembler.getCacheManager();

        String hotKey = cacheKey(Type.A, HOT_DOMAIN);
        String coldKey = cacheKey(Type.A, COLD_DOMAIN);
        Message hotCachedResponse = buildNoErrorEmptyResponseWithSoa(HOT_DOMAIN, Type.A, 90L, 180L);
        Message coldCachedResponse = buildNoErrorEmptyResponseWithSoa(COLD_DOMAIN, Type.A, 90L, 180L);
        fakeUpstreamServer.mockResponse(buildRequest(HOT_DOMAIN, Type.A), hotCachedResponse);
        fakeUpstreamServer.mockResponse(buildRequest(COLD_DOMAIN, Type.A), coldCachedResponse);

        cacheManager.put(hotKey, hotCachedResponse, 1L);
        cacheManager.put(coldKey, coldCachedResponse, 1L);
        cacheManager.get(hotKey);
        cacheManager.get(coldKey);
        cacheManager.get(hotKey);
        eventListener.reset();

        assertNotNull(cacheManager.safeGet(hotKey));
        eventListener.assertCount(CACHE_REWARM, 1);

        Message response = executeCustomRequest(buildRequest(HOT_DOMAIN, Type.A));

        assertNotNull(response);
        assertEquals(NOERROR, response.getRcode());
        eventListener.assertCount(CACHE_HIT, 1, false);
    }

    private Message buildNoErrorEmptyResponse(String domain, int type) {
        Message query = buildRequest(domain, type);
        Message response = new Message(query.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(query.getQuestion(), Section.QUESTION);
        return response;
    }

    private Message buildNoErrorEmptyResponseWithSoa(String domain, int type, long ttl, long minimum) {
        try {
            Message response = buildNoErrorEmptyResponse(domain, type);
            Name zone = response.getQuestion().getName();
            SOARecord soaRecord = new SOARecord(zone, DClass.IN, ttl, Name.fromString("ns1.example.com."), Name.fromString("hostmaster.example.com."), 1L, 3600L, 600L, 86400L, minimum);
            response.addRecord(soaRecord, Section.AUTHORITY);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("failed to build noerror empty response with soa", e);
        }
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
