package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.conf.inner.RecursiveConf;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean.FixedRootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.recursive.infra.ClockWalkingAuthorityQueryClient;
import com.allanvital.dnsao.dns.processor.recursive.infra.PositiveAnswer;
import com.allanvital.dnsao.dns.processor.recursive.infra.RecursiveAuthorityScenario;
import com.allanvital.dnsao.dns.processor.recursive.infra.ReferralAnswer;
import com.allanvital.dnsao.graph.TestTimeProvider;
import com.allanvital.dnsao.infra.clock.Clock;
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
public class RecursiveUnitPerAuthorityTimeoutMillisTest extends AbstractRecursiveUnitTestSupport {

    private DnsQueryRequest request;
    private RecursiveUnit recursiveUnit;
    private RecursiveConf recursiveConf;
    private TestTimeProvider testTimeProvider;

    @BeforeEach
    public void setup() throws Exception {
        resetRecursiveInfra();
        testTimeProvider = TestTimeProvider.getInstance();
        testTimeProvider.setNow(0L);
        Clock.setNewTimeProvider(testTimeProvider);
        request = buildRequest("example.com");
        recursiveConf = new RecursiveConf();

        AuthorityEndpoint rootServer = endpoint("a.root-servers.net.", "198.41.0.4", 53);
        AuthorityEndpoint comServer = endpoint("a.gtld-servers.net.", "192.5.6.30", 53);
        AuthorityEndpoint exampleServer = endpoint("ns1.example.com.", "192.0.2.53", 53);

        ReferralAnswer referralToCom = ReferralAnswer.withGlue("com.", "a.gtld-servers.net.", "192.5.6.30");
        ReferralAnswer referralToExample = ReferralAnswer.withGlue("example.com.", "ns1.example.com.", "192.0.2.53");
        PositiveAnswer finalAnswer = PositiveAnswer.a("example.com.", "93.184.216.34", 300);

        RecursiveAuthorityScenario scenario = new RecursiveAuthorityScenario();
        scenario.whenAsked(rootServer).forQuestion(Type.NS, "com.").returnReferral(referralToCom);
        scenario.whenAsked(comServer).forQuestion(Type.NS, "example.com.").returnReferral(referralToExample);
        scenario.whenAsked(exampleServer).forQuestion(Type.A, "example.com.").returnAnswer(finalAnswer);

        recursiveUnit = buildRecursiveUnit(
                new FixedRootHintsProvider(List.of(rootServer)),
                new ClockWalkingAuthorityQueryClient(scenario, testTimeProvider, 1500L),
                recursiveConf
        );
    }

    @Test
    public void processShouldServfailWhenSingleAuthorityQueryExceedsPerAuthorityTimeout() {
        recursiveConf.setTimeoutSeconds(10);
        recursiveConf.setPerAuthorityTimeoutMillis(1000);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(SERVFAIL, response);
    }

    @Test
    public void processShouldSucceedWhenEachAuthorityQueryStaysWithinPerAuthorityTimeout() {
        recursiveConf.setTimeoutSeconds(10);
        recursiveConf.setPerAuthorityTimeoutMillis(2000);

        DnsQueryResponse response = recursiveUnit.process(request);

        assertNotNull(response);
        assertCodeResponse(NOERROR, response);
    }
}
