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
public class RecursiveUnitCnameCrossZoneTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private RecursiveUnit recursiveUnit;
    private RecursiveAuthorityScenario scenario;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint comServer;
    private AuthorityEndpoint netServer;
    private AuthorityEndpoint exampleComServer;
    private AuthorityEndpoint exampleNetServer;
    private static final String COM_DOT = "com.";
    private static final String NET_DOT = "net.";
    private static final String DOMAIN_DOT = "www.example.com.";
    private static final String TARGET_DOT = "edge.example.net.";
    private static final String FINAL_IP = "198.51.100.10";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("www.example.com");
        RecursiveConf recursiveConf = new RecursiveConf();

        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        comServer = endpoint("a.gtld-servers.net.", "192.5.6.30", 53);
        netServer = endpoint("a.gtld-servers.net.", "192.31.80.30", 53);
        exampleComServer = endpoint("ns1.example.com.", "192.0.2.53", 53);
        exampleNetServer = endpoint("ns1.example.net.", "192.0.2.63", 53);

        ReferralAnswer referralToCom = ReferralAnswer.withGlue(COM_DOT, "a.gtld-servers.net.", "192.5.6.30");
        ReferralAnswer referralToNet = ReferralAnswer.withGlue(NET_DOT, "a.gtld-servers.net.", "192.31.80.30");
        ReferralAnswer referralToExampleCom = ReferralAnswer.withGlue("example.com.", "ns1.example.com.", "192.0.2.53");
        ReferralAnswer referralToExampleNet = ReferralAnswer.withGlue("example.net.", "ns1.example.net.", "192.0.2.63");
        PositiveAnswer cnameAnswer = PositiveAnswer.cname(DOMAIN_DOT, TARGET_DOT, 300);
        PositiveAnswer finalAnswer = PositiveAnswer.a(TARGET_DOT, FINAL_IP, 300);

        scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExampleCom);
        scenario.whenAsked(exampleComServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToExampleCom);
        scenario.whenAsked(exampleComServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(cnameAnswer);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, NET_DOT).returnReferral(referralToNet);
        scenario.whenAsked(netServer).forQuestion(Type.NS, "example.net.").returnReferral(referralToExampleNet);
        scenario.whenAsked(exampleNetServer).forQuestion(Type.NS, TARGET_DOT).returnReferral(referralToExampleNet);
        scenario.whenAsked(exampleNetServer).forQuestion(Type.A, TARGET_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);
    }

    @Test
    public void processShouldFollowCnameAcrossZoneBoundary() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        scenario.assertCallCount(8);
        scenario.assertAuthorityCalledAt(0, rootServer, Type.NS, COM_DOT);
        scenario.assertAuthorityCalledAt(1, comServer, Type.NS, "example.com.");
        scenario.assertAuthorityCalledAt(2, exampleComServer, Type.NS, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(3, exampleComServer, Type.A, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(4, rootServer, Type.NS, NET_DOT);
        scenario.assertAuthorityCalledAt(5, netServer, Type.NS, "example.net.");
        scenario.assertAuthorityCalledAt(6, exampleNetServer, Type.NS, TARGET_DOT);
        scenario.assertAuthorityCalledAt(7, exampleNetServer, Type.A, TARGET_DOT);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }
}
