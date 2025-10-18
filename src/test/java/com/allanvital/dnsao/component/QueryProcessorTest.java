package com.allanvital.dnsao.component;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.QueryProcessor;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.helper.MessageUtils;
import com.allanvital.dnsao.TestHolder;
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
        super.loadConf("1udp-upstream-nocache.yml", false);
        super.startFakeDnsServer();
        super.prepareSimpleMockResponse(domain, responseIp);
        ResolverFactory resolverFactory = new ResolverFactory(null, conf.getResolver().getUpstreams());
        QueryProcessorFactory factory = new QueryProcessorFactory(resolverFactory.getAllResolvers(), null, null, null, 1, DNSSecMode.OFF);
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void shouldRequestUdpCorrectly() throws Exception {
        Message request = MessageUtils.buildARequest(domain);
        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        String responseIp = MessageUtils.extractIpFromResponseMessage(dnsQuery.getResponse());
        Assertions.assertEquals(this.responseIp, responseIp);
    }

    @AfterEach
    public void tearDown() {
        super.stopFakeDnsServer();
    }

}