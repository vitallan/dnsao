package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessorTest extends TestHolder {

    private QueryProcessor processor;
    private String domain = "example.com";
    private String responseIp = "10.10.10.10";

    @BeforeEach
    public void setup() throws Exception {
        safeStart("1udp-upstream-nocache.yml");
        super.prepareSimpleMockResponse(domain, responseIp);
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void shouldRequestUdpCorrectly() throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        String responseIp = MessageHelper.extractIpFromResponseMessage(dnsQuery.getResponse());
        Assertions.assertEquals(this.responseIp, responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}