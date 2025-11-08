package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;

import static com.allanvital.dnsao.graph.bean.MessageHelper.extractIpFromResponseMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * rfc8767
 *
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServeStaleTest extends TestHolder {

    private String domain = "example.com";
    private SimpleResolver resolver;
    private String ip = "5.5.5.5";
    private Message request = MessageHelper.buildARequest(domain);
    private Message response = MessageHelper.buildAResponse(request, ip, 1);

    @BeforeEach
    public void setup() throws IOException, ConfException, InterruptedException {
        safeStart("1udp-upstream-cache-stale.yml");

        resolver = buildResolver(dnsServer.getUdpPort());
        fakeUpstreamServer.mockResponse(request, response);
        Message response = doRequest(resolver, domain);
        assertEquals(ip, extractIpFromResponseMessage(response));
        eventListener.assertCount(EventType.CACHE_ADDED, 1, false);
    }

    @Test
    public void shouldServeStaleWhenConfiguredAndUpstreamFails() throws Exception {
        fakeUpstreamServer.stop();
        testTimeProvider.walkNow(1300);
        response = doRequest(resolver, domain);
        assertEquals(ip, extractIpFromResponseMessage(response));
        assertEquals(30, DnsUtils.getTtlFromDirectResponse(response));
        eventListener.assertCount(EventType.STALE_CACHE_HIT, 1, false);
    }

    @Test
    public void shouldServeStaleWhenConfiguredAndUpstreamReturnsServfail() throws IOException, InterruptedException {
        Message servfail = MessageHelper.buildServfailFrom(request);
        fakeUpstreamServer.mockResponse(request, servfail);
        testTimeProvider.walkNow(1010);

        response = doRequest(resolver, domain);
        assertEquals(ip, extractIpFromResponseMessage(response));
        eventListener.assertCount(EventType.STALE_CACHE_HIT, 1, false);

        testTimeProvider.walkNow(1010);
        response = doRequest(resolver, domain);

        assertEquals(SERVFAIL, response.getHeader().getRcode());
        eventListener.assertCount(EventType.STALE_CACHE_HIT, 1);
    }

    @Test
    public void shouldServeStaleWhenConfiguredAndUpstreamReturnsRefused() throws IOException, InterruptedException {
        Message refused = MessageHelper.buildRefused(request);
        fakeUpstreamServer.mockResponse(request, refused);
        testTimeProvider.walkNow(1010);
        response = doRequest(resolver, domain);
        assertEquals(ip, extractIpFromResponseMessage(response));
        eventListener.assertCount(EventType.STALE_CACHE_HIT, 1, false);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
