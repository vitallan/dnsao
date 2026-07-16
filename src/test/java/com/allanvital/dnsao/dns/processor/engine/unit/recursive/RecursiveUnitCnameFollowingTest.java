package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.conf.inner.RecursiveConf;
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
public class RecursiveUnitCnameFollowingTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private RecursiveUnit recursiveUnit;
    private RecursiveAuthorityScenario scenario;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint comServer;
    private AuthorityEndpoint exampleServer;
    private static final String COM_DOT = "com.";
    private static final String DOMAIN_DOT = "www.example.com.";
    private static final String TARGET_DOT = "origin.example.com.";
    private static final String FINAL_IP = "93.184.216.34";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("www.example.com");
        RecursiveConf recursiveConf = new RecursiveConf();

        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        comServer = endpoint("a.gtld-servers.net.", "192.5.6.30", 53);
        exampleServer = endpoint("ns1.example.com.", "192.0.2.53", 53);

        ReferralAnswer referralToCom = ReferralAnswer.withGlue(COM_DOT, "a.gtld-servers.net.", "192.5.6.30");
        ReferralAnswer referralToExample = ReferralAnswer.withGlue("example.com.", "ns1.example.com.", "192.0.2.53");
        PositiveAnswer cnameAnswer = PositiveAnswer.cname(DOMAIN_DOT, TARGET_DOT, 300);
        PositiveAnswer finalAnswer = PositiveAnswer.a(TARGET_DOT, FINAL_IP, 300);

        scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(cnameAnswer);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.NS, TARGET_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, TARGET_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);
    }

    @Test
    public void processShouldFollowCnameWithinSameZone() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        scenario.assertCallCount(8);
        scenario.assertAuthorityCalledAt(0, rootServer, Type.NS, COM_DOT);
        scenario.assertAuthorityCalledAt(1, comServer, Type.NS, "example.com.");
        scenario.assertAuthorityCalledAt(2, exampleServer, Type.NS, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(3, exampleServer, Type.A, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(4, rootServer, Type.NS, COM_DOT);
        scenario.assertAuthorityCalledAt(5, comServer, Type.NS, "example.com.");
        scenario.assertAuthorityCalledAt(6, exampleServer, Type.NS, TARGET_DOT);
        scenario.assertAuthorityCalledAt(7, exampleServer, Type.A, TARGET_DOT);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }
}
