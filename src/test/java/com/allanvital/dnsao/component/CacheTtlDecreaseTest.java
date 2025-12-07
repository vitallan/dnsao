package com.allanvital.dnsao.component;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;

import static com.allanvital.dnsao.graph.bean.MessageHelper.getTtlFromResponse;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_ADDED;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.QUERY_RESOLVED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheTtlDecreaseTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        safeStart("1udp-upstream-cache.yml");
    }

    @Test
    public void ttlShouldDecayWhenServedFromCache() throws IOException, InterruptedException {
        long ttl = 300;
        super.prepareSimpleMockResponse(domain, "10.10.10.10", ttl);
        Message response = executeRequestOnOwnServer(dnsServer, domain, false);
        Long ttlFromResponse = getTtlFromResponse(response);
        assertEquals(ttl, ttlFromResponse);
        eventListener.assertCount(CACHE_ADDED, 1, false);

        testTimeProvider.walkNow(1000);
        response = executeRequestOnOwnServer(dnsServer, domain, false);
        ttlFromResponse = getTtlFromResponse(response);
        assertEquals(ttl - 1, (long) ttlFromResponse, "ttlFromResponse=" + ttlFromResponse);
        eventListener.assertCount(QUERY_RESOLVED, 2, false);

        testTimeProvider.walkNow(1000);
        response = executeRequestOnOwnServer(dnsServer, domain, false);
        ttlFromResponse = getTtlFromResponse(response);
        assertEquals(ttl - 2, (long) ttlFromResponse, "ttlFromResponse=" + ttlFromResponse);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
