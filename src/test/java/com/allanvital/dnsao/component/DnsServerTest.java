package com.allanvital.dnsao.component;

import com.allanvital.dnsao.graph.SystemGraph;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServerTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        safeStart("1udp-upstream-nocache.yml");
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
        Message response = executeRequestOnOwnServer(dnsServer, domain, tcp);
        String responseIp = MessageHelper.extractIpFromResponseMessage(response);
        Assertions.assertEquals(expectedIp, responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}