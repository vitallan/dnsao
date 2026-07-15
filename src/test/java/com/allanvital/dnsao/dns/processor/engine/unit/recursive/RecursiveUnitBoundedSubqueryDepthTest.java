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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.xbill.DNS.Rcode.NOERROR;
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnitBoundedSubqueryDepthTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private RecursiveUnit recursiveUnit;
    private RecursiveConf recursiveConf;
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
    private static final String FINAL_IP = "198.51.100.77";

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        request = buildRequest("pudim.com.br");
        recursiveConf = new RecursiveConf();

        AuthorityEndpoint rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        AuthorityEndpoint brServer = endpoint(BR_NS_URL, "192.0.2.10", 53);
        AuthorityEndpoint comBrServer = endpoint(COM_BR_NS_URL, "192.0.2.20", 53);
        AuthorityEndpoint netServer = endpoint(NET_NS_URL, "192.0.2.30", 53);
        AuthorityEndpoint dnsHostServer = endpoint(DNS_HOST_NS_URL, "192.0.2.40", 53);
        AuthorityEndpoint noGlueAuthorityServer = endpoint(NO_GLUE_NS_URL, "192.0.2.50", 53);

        ReferralAnswer referralToBr = ReferralAnswer.withGlue(BR_DOT, BR_NS_URL, "192.0.2.10");
        ReferralAnswer referralToComBr = ReferralAnswer.withGlue(COM_BR_DOT, COM_BR_NS_URL, "192.0.2.20");
        ReferralAnswer referralToPudimWithoutGlue = ReferralAnswer.withoutGlue(DOMAIN_DOT, NO_GLUE_NS_URL);
        ReferralAnswer referralToNet = ReferralAnswer.withGlue(NET_DOT, NET_NS_URL, "192.0.2.30");
        ReferralAnswer referralToDnsHost = ReferralAnswer.withGlue(DNS_HOST_DOT, DNS_HOST_NS_URL, "192.0.2.40");
        PositiveAnswer nsAddressAnswer = PositiveAnswer.a(NO_GLUE_NS_URL, "192.0.2.50", 300);
        PositiveAnswer finalAnswer = PositiveAnswer.a(DOMAIN_DOT, FINAL_IP, 300);

        RecursiveAuthorityScenario scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, BR_DOT).returnReferral(referralToBr);
        scenario.whenAsked(brServer).forQuestion(Type.NS, COM_BR_DOT).returnReferral(referralToComBr);
        scenario.whenAsked(comBrServer).forQuestion(Type.NS, DOMAIN_DOT).returnReferral(referralToPudimWithoutGlue);
        scenario.whenAsked(rootServer).forQuestion(Type.NS, NET_DOT).returnReferral(referralToNet);
        scenario.whenAsked(netServer).forQuestion(Type.NS, DNS_HOST_DOT).returnReferral(referralToDnsHost);
        scenario.whenAsked(dnsHostServer).forQuestion(Type.A, NO_GLUE_NS_URL).returnAnswer(nsAddressAnswer);
        scenario.whenAsked(noGlueAuthorityServer).forQuestion(Type.A, DOMAIN_DOT).returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(new FixedRootHintsProvider(List.of(rootServer)), scenario, recursiveConf);
    }

    @Test
    public void processShouldServfailWhenSubqueryDepthIsTooLow() {
        recursiveConf.setMaxSubqueryDepth(0);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(SERVFAIL, response);
    }

    @Test
    public void processShouldSucceedWhenSubqueryDepthAllowsOneChildSession() {
        recursiveConf.setMaxSubqueryDepth(1);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
    }
}
