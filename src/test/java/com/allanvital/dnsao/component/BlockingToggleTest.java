package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.FakeDownloadDomainListFileReader;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class BlockingToggleTest extends TestHolder {

    private static final String BLOCKED_DOMAIN = "block1.com";
    private static final String BLOCK_IP = "0.0.0.0";
    private static final String RESOLVED_IP = "10.10.10.10";

    private QueryProcessor processor;

    @BeforeEach
    public void setup() throws ConfException {
        FakeDownloadDomainListFileReader fileReader = new FakeDownloadDomainListFileReader();
        registerOverride(fileReader);
    }

    @Test
    void shouldBlockWhenBlockingEnabledNotConfigured() throws Exception {
        startWithConf("blocking-enabled-default.yml");

        DnsQuery response = processor.processExternalQuery(
                Inet4Address.getByName("127.0.0.1"),
                MessageHelper.buildARequest(BLOCKED_DOMAIN).toWire());

        String responseIp = MessageHelper.extractIpFromResponseMessage(response.getResponse());
        assertEquals(BLOCK_IP, responseIp);
    }

    @Test
    void shouldBlockWhenBlockingEnabledExplicitlyTrue() throws Exception {
        startWithConf("blocking-enabled-true.yml");

        DnsQuery response = processor.processExternalQuery(
                Inet4Address.getByName("127.0.0.1"),
                MessageHelper.buildARequest(BLOCKED_DOMAIN).toWire());

        String responseIp = MessageHelper.extractIpFromResponseMessage(response.getResponse());
        assertEquals(BLOCK_IP, responseIp);
    }

    @Test
    void shouldNotBlockWhenBlockingEnabledFalse() throws Exception {
        startWithConf("blocking-enabled-false.yml");
        DnsQuery response = processor.processExternalQuery(
                Inet4Address.getByName("127.0.0.1"),
                MessageHelper.buildARequest(BLOCKED_DOMAIN).toWire());

        String responseIp = MessageHelper.extractIpFromResponseMessage(response.getResponse());
        assertEquals(RESOLVED_IP, responseIp);
    }

    private void startWithConf(String conf) throws Exception {
        safeStart(conf);
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
        prepareSimpleMockResponse(BLOCKED_DOMAIN, RESOLVED_IP);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
