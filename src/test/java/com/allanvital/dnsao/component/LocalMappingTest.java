package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.dns.server.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LocalMappingTest extends TestHolder {

    private String domain1 = "karpov";
    private String domain2 = "another.internal.domain";
    private DnsServer realServer;

    @BeforeEach
    public void setup() throws ConfException {
        super.loadConf("local-mappings.yml", true);
        realServer = systemGraph.getDnsServer();
        realServer.start();
    }

    @Test
    public void shouldAnswerWithLocalMappingsWhenAvailable() throws IOException {
        Message response = executeRequestOnDnsao(realServer, domain1, false);
        String responseIp = MessageUtils.extractIpFromResponseMessage(response);
        Assertions.assertEquals("192.168.168.168", responseIp);

        response = executeRequestOnDnsao(realServer, domain2, false);
        responseIp = MessageUtils.extractIpFromResponseMessage(response);
        Assertions.assertEquals("192.168.168.169", responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        if (realServer != null) {
            realServer.stop();
            realServer = null;
        }
    }

}
