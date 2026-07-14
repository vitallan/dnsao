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
public class RecursiveUnitHappyPathWithGlueDeepDomainTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint brServer;
    private AuthorityEndpoint comBrServer;
    private AuthorityEndpoint pudimServer;
    private RecursiveAuthorityScenario scenario;
    private RecursiveUnit recursiveUnit;
    private static final String BR = "br";
    private static final String BR_DOT = BR + ".";
    private static final String COM_BR = "com." + BR;
    private static final String COM_BR_DOT = COM_BR + ".";
    private static final String DOMAIN = "pudim." + COM_BR;
    private static final String DOMAIN_DOT = DOMAIN + ".";
    private static final String BR_NS_URL = "a.dns.br.";
    private static final String COM_BR_NS_URL = "a.dns.com.br.";
    private static final String PUDIM_NS_URL = "ns1.pudim.com.br.";
    private static final String BR_NS_IP = "192.0.2.10";
    private static final String COM_BR_NS_IP = "192.0.2.20";
    private static final String PUDIM_NS_IP = "192.0.2.30";
    private static final String FINAL_IP = "198.51.100.77";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest(DOMAIN);
        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        brServer = endpoint(BR_NS_URL, BR_NS_IP, 53);
        comBrServer = endpoint(COM_BR_NS_URL, COM_BR_NS_IP, 53);
        pudimServer = endpoint(PUDIM_NS_URL, PUDIM_NS_IP, 53);

        ReferralAnswer referralToBr = new ReferralAnswer(BR_DOT);
        referralToBr.addNameserver(BR_NS_URL);
        referralToBr.addGlueA(BR_NS_URL, BR_NS_IP);

        ReferralAnswer referralToComBr = new ReferralAnswer(COM_BR_DOT);
        referralToComBr.addNameserver(COM_BR_NS_URL);
        referralToComBr.addGlueA(COM_BR_NS_URL, COM_BR_NS_IP);

        ReferralAnswer referralToPudim = new ReferralAnswer(DOMAIN_DOT);
        referralToPudim.addNameserver(PUDIM_NS_URL);
        referralToPudim.addGlueA(PUDIM_NS_URL, PUDIM_NS_IP);

        PositiveAnswer finalAnswer = new PositiveAnswer(Type.A, DOMAIN_DOT, FINAL_IP, 300);

        scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, BR_DOT).returnReferral(referralToBr);
        scenario.whenAsked(brServer).forQuestion(Type.NS, COM_BR_DOT).returnReferral(referralToComBr);
        scenario.whenAsked(comBrServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToPudim);
        scenario.whenAsked(pudimServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario);
    }

    @Test
    public void processShouldResolvePudimComBrHappyPathWithGlue() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        scenario.assertCallCount(4);
        scenario.assertAuthorityCalledAt(0, rootServer, Type.NS, BR_DOT);
        scenario.assertAuthorityCalledAt(1, brServer, Type.NS, COM_BR_DOT);
        scenario.assertAuthorityCalledAt(2, comBrServer, Type.NS, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(3, pudimServer, Type.A, DOMAIN_DOT);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }
}
