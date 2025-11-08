package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class IdIsolationTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        safeStart("1udp-upstream-nocache.yml");
    }

    @Test
    public void originalQueryIdShouldNotBePropagatedUpstream() throws IOException {
        String ip = "1.1.1.1";
        Message request = MessageHelper.buildARequest(domain);
        int clientRequestId = request.getHeader().getID();
        Message mockResponse = MessageHelper.buildAResponse(request, ip, 10);
        fakeUpstreamServer.mockResponse(request, mockResponse);

        Resolver resolver = super.buildResolver(dnsServer.getUdpPort());
        Message response = resolver.send(request);

        String responseIp = MessageHelper.extractIpFromResponseMessage(response);
        assertEquals(ip, responseIp);

        int lastIdOnRemoteServer = fakeUpstreamServer.getLastRequestId();
        assertNotEquals(0, lastIdOnRemoteServer);
        assertNotEquals(clientRequestId, lastIdOnRemoteServer, "the original query id should not be propagated upstream");
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
