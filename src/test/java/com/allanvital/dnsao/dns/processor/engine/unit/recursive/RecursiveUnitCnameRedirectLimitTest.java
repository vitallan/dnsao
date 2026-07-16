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
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnitCnameRedirectLimitTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint comServer;
    private AuthorityEndpoint exampleServer;
    private static final String COM_DOT = "com.";
    private static final String FIRST_DOT = "a.example.com.";
    private static final String SECOND_DOT = "b.example.com.";
    private static final String THIRD_DOT = "c.example.com.";
    private static final String FINAL_IP = "93.184.216.34";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("a.example.com");
        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        comServer = endpoint("a.gtld-servers.net.", "192.5.6.30", 53);
        exampleServer = endpoint("ns1.example.com.", "192.0.2.53", 53);
    }

    @Test
    public void processShouldServfailWhenCnameRedirectLimitIsExceeded() {
        RecursiveConf recursiveConf = new RecursiveConf();
        recursiveConf.setMaxCnameRedirects(1);

        RecursiveAuthorityScenario scenario = buildCnameChainScenario();
        RecursiveUnit recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(SERVFAIL, response);
    }

    @Test
    public void processShouldSucceedWhenCnameRedirectLimitAllowsTheChain() {
        RecursiveConf recursiveConf = new RecursiveConf();
        recursiveConf.setMaxCnameRedirects(2);

        RecursiveAuthorityScenario scenario = buildCnameChainScenario();
        RecursiveUnit recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }

    private RecursiveAuthorityScenario buildCnameChainScenario() {
        ReferralAnswer referralToCom = ReferralAnswer.withGlue(COM_DOT, "a.gtld-servers.net.", "192.5.6.30");
        ReferralAnswer referralToExample = ReferralAnswer.withGlue("example.com.", "ns1.example.com.", "192.0.2.53");
        PositiveAnswer firstCname = PositiveAnswer.cname(FIRST_DOT, SECOND_DOT, 300);
        PositiveAnswer secondCname = PositiveAnswer.cname(SECOND_DOT, THIRD_DOT, 300);
        PositiveAnswer finalAnswer = PositiveAnswer.a(THIRD_DOT, FINAL_IP, 300);

        RecursiveAuthorityScenario scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.NS, FIRST_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, FIRST_DOT).returnAnswer(firstCname);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.NS, SECOND_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, SECOND_DOT).returnAnswer(secondCname);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.NS, THIRD_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, THIRD_DOT).returnAnswer(finalAnswer);
        return scenario;
    }
}
