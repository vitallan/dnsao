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

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServerTest extends TestHolder {

    private String domain = "example.com";
    private DnsServer realServer;
    private SystemGraph systemGraph;

    @BeforeEach
    public void setup() throws ConfException {
        super.loadConf("1udp-upstream-nocache.yml", true);
        super.startFakeDnsServer();
        systemGraph = new SystemGraph(conf);
        realServer = systemGraph.getDnsServer();
        realServer.start();
    }

    @Test
    public void testUdpRemoteRequest() throws IOException {
        String expectedIp = "10.10.10.10";
        sendRequestAndValidateIpResponse(expectedIp, false);
    }

    @Test
    public void testTcpRemoteRequest() throws IOException {
        String expectedIp = "10.10.10.20";
        sendRequestAndValidateIpResponse(expectedIp, true);
    }

    private void sendRequestAndValidateIpResponse(String expectedIp, boolean tcp) throws IOException {
        super.prepareSimpleMockResponse(domain, expectedIp);
        Message response = executeRequestOnDnsao(realServer, domain, tcp);
        String responseIp = MessageUtils.extractIpFromResponseMessage(response);
        Assertions.assertEquals(expectedIp, responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (systemGraph != null) {
            systemGraph.stop();
            realServer = null;
        }
        super.stopFakeDnsServer();
    }

}