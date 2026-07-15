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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xbill.DNS.Rcode.NOERROR;
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnitBoundedStepsSimpleDomainTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private RecursiveUnit recursiveUnit;
    private RecursiveConf recursiveConf;
    private static final String COM_DOT = "com.";
    private static final String DOMAIN = "example.com";
    private static final String DOMAIN_DOT = DOMAIN + ".";
    private static final String FINAL_IP = "93.184.216.34";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest(DOMAIN);
        recursiveConf = new RecursiveConf();

        AuthorityEndpoint rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        AuthorityEndpoint comServer = endpoint("a.gtld-servers.net.", "192.5.6.30", 53);
        AuthorityEndpoint exampleServer = endpoint("ns1.example.com.", "192.0.2.53", 53);

        ReferralAnswer referralToCom = ReferralAnswer.withGlue(COM_DOT, "a.gtld-servers.net.", "192.5.6.30");
        ReferralAnswer referralToExample = ReferralAnswer.withGlue(DOMAIN_DOT, "ns1.example.com.", "192.0.2.53");
        PositiveAnswer finalAnswer = PositiveAnswer.a(DOMAIN_DOT, FINAL_IP, 300);

        RecursiveAuthorityScenario scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, COM_DOT).returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);
    }

    @Test
    public void processShouldServfailWhenMaxStepsIsTooLowForExampleCom() {
        recursiveConf.setMaxSteps(2);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(SERVFAIL, response);
    }

    @Test
    public void processShouldSucceedWhenMaxStepsMatchesExampleComPath() {
        recursiveConf.setMaxSteps(3);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        assertEquals(FINAL_IP, com.allanvital.dnsao.graph.bean.MessageHelper.extractIpFromDnsQueryResponse(response));
    }
}
