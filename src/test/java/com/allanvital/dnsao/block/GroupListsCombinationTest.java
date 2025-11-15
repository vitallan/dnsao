package com.allanvital.dnsao.block;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.graph.bean.FakeDownloadDomainListFileReader;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.InetAddress;

import static com.allanvital.dnsao.graph.bean.MessageHelper.buildARequest;
import static com.allanvital.dnsao.graph.bean.MessageHelper.extractIpFromResponseMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class GroupListsCombinationTest extends TestHolder {

    private static final String IP = "10.10.10.10";
    private static final String BLOCK = "0.0.0.0";

    private static final String BLOCK_1 = "block1.com";
    private static final String BLOCK_2 = "block2.com";
    private static final String BLOCK_SUBDOMAIN = "sub.block1.com";
    private static final String ALLOW_1 = "allow1.com";
    private static final String ALLOW_2 = "allow2.com";
    private static final String BLOCK_ALLOW_CONFLICT = "conflict.com";
    private static final String BLOCK_ALLOW_SUBDOMAIN_CONFLICT = "sub.conflict.com";

    private static final String GROUP_1_IP = "127.0.0.5";
    private static final String GROUP_2_IP = "127.0.0.10";
    private static final String GROUP_3_IP = "127.0.0.8";

    private QueryProcessor processor;

    @BeforeEach
    public void setup() throws Exception {
        FakeDownloadDomainListFileReader fileReader = new FakeDownloadDomainListFileReader();
        registerOverride(fileReader);
        safeStart("1udp-2groups.yml");
        prepareSimpleMockResponse(BLOCK_1, IP);
        prepareSimpleMockResponse(BLOCK_2, IP);
        prepareSimpleMockResponse(ALLOW_1, IP);
        prepareSimpleMockResponse(ALLOW_2, IP);
        prepareSimpleMockResponse(BLOCK_ALLOW_CONFLICT, IP);
        prepareSimpleMockResponse(BLOCK_SUBDOMAIN, IP);
        prepareSimpleMockResponse(BLOCK_ALLOW_SUBDOMAIN_CONFLICT, IP);
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void blockOutsideAnyGroup() throws Exception {
        doTest("127.0.0.50", BLOCK, BLOCK, IP, IP); //MAIN test
        doTest(GROUP_1_IP, BLOCK, BLOCK, IP, IP);
        doTest(GROUP_2_IP, BLOCK, IP, IP, IP);
        doTest(GROUP_3_IP, IP, IP, IP, IP);
    }

    @Test
    public void blockSubdomainDefault() throws Exception {
        requestAndAssert("127.0.0.1", BLOCK_SUBDOMAIN, BLOCK);
    }

    @Test
    public void allowBlockAndAllowConflict() throws Exception {
        requestAndAssert("127.0.0.1", BLOCK_ALLOW_CONFLICT, IP);
    }

    @Test
    public void blockBlockAndAllowConflictOnSubdomain() throws Exception {
        requestAndAssert("127.0.0.1", BLOCK_ALLOW_SUBDOMAIN_CONFLICT, BLOCK);
    }

    private void doTest(String clientIp, String response1, String response2, String response3, String response4) throws Exception {
        requestAndAssert(clientIp, BLOCK_1, response1);
        requestAndAssert(clientIp, BLOCK_2, response2);
        requestAndAssert(clientIp, ALLOW_1, response3);
        requestAndAssert(clientIp, ALLOW_2, response4);
    }

    private void requestAndAssert(String clientIp, String domain, String expectedResponseIp) throws Exception {
        InetAddress client = Inet4Address.getByName(clientIp);
        DnsQuery response = processor.processExternalQuery(client, buildARequest(domain).toWire());
        String responseIp = extractIpFromResponseMessage(response.getResponse());
        assertEquals(expectedResponseIp, responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
