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
public class RecursiveUnitHappyPathWithoutGlueDeepDomainTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private AuthorityEndpoint rootServer;
    private AuthorityEndpoint brServer;
    private AuthorityEndpoint comBrServer;
    private AuthorityEndpoint netServer;
    private AuthorityEndpoint dnsHostServer;
    private AuthorityEndpoint noGlueAuthorityServer;
    private RecursiveAuthorityScenario scenario;
    private RecursiveUnit recursiveUnit;
    private static final String BR_DOT = "br.";
    private static final String COM_BR_DOT = "com.br.";
    private static final String NET_DOT = "net.";
    private static final String DNS_HOST_DOT = "dns-host.net.";
    private static final String DOMAIN_DOT = "pudim.com.br.";
    private static final String BR_NS_URL = "a.dns.br.";
    private static final String COM_BR_NS_URL = "a.dns.com.br.";
    private static final String NET_NS_URL = "a.gtld-servers.net.";
    private static final String DNS_HOST_NS_URL = "a.dns-host.net.";
    private static final String NO_GLUE_NS_URL = "dns-host.net.";
    private static final String BR_NS_IP = "192.0.2.10";
    private static final String COM_BR_NS_IP = "192.0.2.20";
    private static final String NET_NS_IP = "192.0.2.30";
    private static final String DNS_HOST_NS_IP = "192.0.2.40";
    private static final String NO_GLUE_NS_IP = "192.0.2.50";
    private static final String FINAL_IP = "198.51.100.77";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("pudim.com.br");
        rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        brServer = endpoint(BR_NS_URL, BR_NS_IP, 53);
        comBrServer = endpoint(COM_BR_NS_URL, COM_BR_NS_IP, 53);
        netServer = endpoint(NET_NS_URL, NET_NS_IP, 53);
        dnsHostServer = endpoint(DNS_HOST_NS_URL, DNS_HOST_NS_IP, 53);
        noGlueAuthorityServer = endpoint(NO_GLUE_NS_URL, NO_GLUE_NS_IP, 53);

        ReferralAnswer referralToBr = new ReferralAnswer(BR_DOT);
        referralToBr.addNameserver(BR_NS_URL);
        referralToBr.addGlueA(BR_NS_URL, BR_NS_IP);

        ReferralAnswer referralToComBr = new ReferralAnswer(COM_BR_DOT);
        referralToComBr.addNameserver(COM_BR_NS_URL);
        referralToComBr.addGlueA(COM_BR_NS_URL, COM_BR_NS_IP);

        ReferralAnswer referralToPudimWithoutGlue = new ReferralAnswer(DOMAIN_DOT);
        referralToPudimWithoutGlue.addNameserver(NO_GLUE_NS_URL);

        ReferralAnswer referralToNet = new ReferralAnswer(NET_DOT);
        referralToNet.addNameserver(NET_NS_URL);
        referralToNet.addGlueA(NET_NS_URL, NET_NS_IP);

        ReferralAnswer referralToDnsHost = new ReferralAnswer(DNS_HOST_DOT);
        referralToDnsHost.addNameserver(DNS_HOST_NS_URL);
        referralToDnsHost.addGlueA(DNS_HOST_NS_URL, DNS_HOST_NS_IP);

        PositiveAnswer nsAddressAnswer = new PositiveAnswer(Type.A, NO_GLUE_NS_URL, NO_GLUE_NS_IP, 300);
        PositiveAnswer finalAnswer = new PositiveAnswer(Type.A, DOMAIN_DOT, FINAL_IP, 300);

        scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, BR_DOT).returnReferral(referralToBr);
        scenario.whenAsked(brServer).forQuestion(Type.NS, COM_BR_DOT).returnReferral(referralToComBr);
        scenario.whenAsked(comBrServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToPudimWithoutGlue);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, NET_DOT).returnReferral(referralToNet);
        scenario.whenAsked(netServer).forQuestion(Type.NS, DNS_HOST_DOT).returnReferral(referralToDnsHost);
        scenario.whenAsked(dnsHostServer).forQuestion(Type.A, NO_GLUE_NS_URL).returnAnswer(nsAddressAnswer);
        scenario.whenAsked(noGlueAuthorityServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario);
    }

    @Test
    public void processShouldResolvePudimComBrHappyPathWithoutGlueAtFinalDelegation() {
        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
        scenario.assertCallCount(7);
        scenario.assertAuthorityCalledAt(0, rootServer, Type.NS, BR_DOT);
        scenario.assertAuthorityCalledAt(1, brServer, Type.NS, COM_BR_DOT);
        scenario.assertAuthorityCalledAt(2, comBrServer, Type.NS, DOMAIN_DOT);
        scenario.assertAuthorityCalledAt(3, rootServer, Type.NS, NET_DOT);
        scenario.assertAuthorityCalledAt(4, netServer, Type.NS, DNS_HOST_DOT);
        scenario.assertAuthorityCalledAt(5, dnsHostServer, Type.A, NO_GLUE_NS_URL);
        scenario.assertAuthorityCalledAt(6, noGlueAuthorityServer, Type.A, DOMAIN_DOT);
        scenario.assertAuthorityCalledTimes(rootServer, 1, Type.NS, BR_DOT);
        scenario.assertAuthorityCalledTimes(rootServer, 1, Type.NS, NET_DOT);
        assertEquals(FINAL_IP, extractIpFromDnsQueryResponse(response));
    }
}
