package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.exc.ConfException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;

import static com.allanvital.dnsao.dns.remote.DnsUtils.getTtlFromDirectResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Long ttlFromResponse = getTtlFromDirectResponse(response);
        assertEquals(ttl, ttlFromResponse);

        testTimeProvider.walkOneSecond();
        response = executeRequestOnOwnServer(dnsServer, domain, false);
        ttlFromResponse = getTtlFromDirectResponse(response);
        assertTrue(ttlFromResponse <= ttl - 1, "ttlFromResponse=" + ttlFromResponse);

        testTimeProvider.walkOneSecond();
        response = executeRequestOnOwnServer(dnsServer, domain, false);
        ttlFromResponse = getTtlFromDirectResponse(response);
        assertTrue(ttlFromResponse <= ttl - 2, "ttlFromResponse=" + ttlFromResponse);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
