package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
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

    @BeforeEach
    public void setup() throws ConfException {
        safeStart("local-mappings.yml");
    }

    @Test
    public void shouldAnswerWithLocalMappingsWhenAvailable() throws IOException {
        Message response = executeRequestOnOwnServer(dnsServer, domain1, false);
        String responseIp = MessageHelper.extractIpFromResponseMessage(response);
        Assertions.assertEquals("192.168.168.168", responseIp);

        response = executeRequestOnOwnServer(dnsServer, domain2, false);
        responseIp = MessageHelper.extractIpFromResponseMessage(response);
        Assertions.assertEquals("192.168.168.169", responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
