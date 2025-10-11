package com.allanvital.dnsao.component;

import com.allanvital.dnsao.SystemGraph;
import com.allanvital.dnsao.dns.server.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.helper.MessageUtils;
import com.allanvital.dnsao.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServerCacheTest extends TestHolder {

    private String domain = "example.com";
    private DnsServer realServer;
    private SimpleResolver resolver;

    @BeforeEach
    public void setup() throws UnknownHostException, ConfException {
        super.loadConf("1udp-upstream-cache.yml", false);
        super.startFakeDnsServer();
        systemGraph = new SystemGraph(conf);
        realServer = systemGraph.getDnsServer();
        realServer.start();
        resolver = buildResolver(realServer.getUdpPort());
    }

    @Test
    public void shouldHitCacheWhenEnabled() throws IOException {
        String expectedIp = "10.10.10.10";
        super.prepareSimpleMockResponse(domain, expectedIp, 10000);
        for (int i = 0; i < 10; i++) {
            Message request = MessageUtils.buildARequest(domain);
            Message response = resolver.send(request);
            String responseIp = MessageUtils.extractIpFromResponseMessage(response);
            Assertions.assertEquals(expectedIp, responseIp);
        }
        Assertions.assertEquals(1, fakeDnsServer.getCallCount());
    }

    @Test
    public void shouldAutomaticallyRemoveOldestFromCache() throws IOException {
        String domain1 = "domain1.com";
        String domain2 = "domain2.com";
        String domain3 = "domain3.com";
        super.prepareSimpleMockResponse(domain1, "10.10.10.10", 10000);
        super.prepareSimpleMockResponse(domain2, "10.10.10.20", 10000);
        super.prepareSimpleMockResponse(domain3, "10.10.10.30", 10000);

        doRequest(resolver, domain1);
        doRequest(resolver, domain2);
        doRequest(resolver, domain3);

        for (int i = 0; i < 10; i++) {
            doRequest(resolver, domain1);
        }
        Assertions.assertEquals(4, fakeDnsServer.getCallCount());
    }

    @Test
    public void shouldAutomaticallyRemoveOldestAccessedFromCache() throws IOException {
        String domain1 = "domain1.com";
        String domain2 = "domain2.com";
        String domain3 = "domain3.com";
        super.prepareSimpleMockResponse(domain1, "10.10.10.10", 10000);
        super.prepareSimpleMockResponse(domain2, "10.10.10.20", 10000);
        super.prepareSimpleMockResponse(domain3, "10.10.10.30", 10000);

        doRequest(resolver, domain1);
        doRequest(resolver, domain2);
        doRequest(resolver, domain1);
        doRequest(resolver, domain3);
        doRequest(resolver, domain1);

        Assertions.assertEquals(3, fakeDnsServer.getCallCount());
    }

    @Test
    public void shouldRespectTtlLimits() throws IOException, InterruptedException {
        super.prepareSimpleMockResponse(domain, "10.10.10.10", 1);
        doRequest(resolver, domain);
        Thread.sleep(1100);
        doRequest(resolver, domain);
        Assertions.assertEquals(2, fakeDnsServer.getCallCount());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (realServer != null) {
            realServer.stop();
            realServer = null;
        }
        super.stopFakeDnsServer();
        eventListener.reset();
    }

}