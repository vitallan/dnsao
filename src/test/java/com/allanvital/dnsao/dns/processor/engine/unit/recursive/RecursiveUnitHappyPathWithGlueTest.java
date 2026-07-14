package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean.FixedRootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.recursive.infra.PositiveAnswer;
import com.allanvital.dnsao.dns.processor.recursive.infra.RecursiveAuthorityScenario;
import com.allanvital.dnsao.dns.processor.recursive.infra.ReferralAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Type;

import java.util.List;

import static com.allanvital.dnsao.graph.bean.MessageHelper.extractIpFromDnsQueryResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xbill.DNS.Rcode.NOERROR;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnitHappyPathWithGlueTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint comServer;
    private AuthorityEndpoint exampleServer;
    private RecursiveAuthorityScenario scenario;
    private RecursiveUnit recursiveUnit;
    private static final String COM = "com";
    private static final String COM_DOT = COM + ".";
    private static final String DOMAIN = "example." + COM;
    private static final String DOMAIN_DOT = DOMAIN + ".";
    private static final String NAMESERVER_IP = "192.0.2.53";
    private static final String FINAL_IP = "93.184.216.34";
    private static final String COM_SERVER_URL = "a.gtld-servers.net.";
    private static final String NS_SERVER_URL = "ns1.example.com.";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest(DOMAIN);
        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        comServer = endpoint(COM_SERVER_URL, "192.5.6.30", 53);
        exampleServer = endpoint(NS_SERVER_URL, NAMESERVER_IP, 53);

        ReferralAnswer referralToCom = new ReferralAnswer(COM_DOT);
        referralToCom.addNameserver(COM_SERVER_URL);
        referralToCom.addGlueA(COM_SERVER_URL, "192.5.6.30");

        ReferralAnswer referralToExample = new ReferralAnswer(DOMAIN_DOT);
        referralToExample.addNameserver(NS_SERVER_URL);
        referralToExample.addGlueA(NS_SERVER_URL, NAMESERVER_IP);

        PositiveAnswer finalAnswer = new PositiveAnswer(Type.A, DOMAIN_DOT, FINAL_IP, 300);

        scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario);
    }

    @Test
    public void processShouldResolveExampleComHappyPathWithGlue() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        scenario.assertCallCount(3);
        scenario.assertAuthorityCalledAt(0, rootServer, Type.NS, COM_DOT);
        scenario.assertAuthorityCalledAt(1, comServer, Type.NS, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(2, exampleServer, Type.A, DOMAIN_DOT);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }
}
