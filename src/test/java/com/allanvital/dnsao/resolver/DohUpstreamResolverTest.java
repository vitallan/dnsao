package com.allanvital.dnsao.resolver;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.DohTestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DohUpstreamResolverTest  extends DohTestHolder {

    @BeforeEach
    public void setup() throws Exception {
        safeStart("doh/1doh-upstream-nocache.yml");
    }

    @Test
    public void shouldHitUpstreamWithDoh() throws IOException {
        Message response = super.executeRequestOnOwnServer(dnsServer, "example.com", false);
        assertEquals(SERVFAIL, response.getHeader().getRcode());
    }

    @Test
    public void testWithMockResponse() throws IOException {
        String domain = "dohexample.com";
        String ip = "10.10.10.10";
        super.prepareSimpleMockResponse(domain, ip);
        Message response = super.executeRequestOnOwnServer(dnsServer, domain, false);
        assertEquals(ip, MessageHelper.extractIpFromResponseMessage(response));
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

}
