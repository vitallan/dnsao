package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveAuthoritativeNsInAuthorityFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthoritativeNsInAuthorityIsNotReferralTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "authoritative-ns-authority.com";
    private static final String DELEGATED_NAMESERVER_HOST = "ns1.authoritative-ns-authority.com";
    private static final String DELEGATED_NAMESERVER_IP = "127.0.0.81";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final String HELPER_ZONE_NS_HOST = "ns1.helper.net";
    private static final String HELPER_ZONE_NS_IP = "127.0.0.1";
    private static final String AUTHORITY_NS_HOST = "ns2.helper.net";
    private static final long TTL = 300;

    private FakeServer delegatedServer;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(DELEGATED_NAMESERVER_IP, delegatedServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveAuthoritativeNsInAuthorityFixture fixture = new RecursiveAuthoritativeNsInAuthorityFixture(fakeUpstreamServer, delegatedServer);
        fixture.load(
                DOMAIN,
                DELEGATED_NAMESERVER_HOST,
                DELEGATED_NAMESERVER_IP,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                HELPER_ZONE_NS_HOST,
                HELPER_ZONE_NS_IP,
                AUTHORITY_NS_HOST,
                TTL
        );
    }

    @Test
    public void authoritativeAnswerWithNsInAuthorityDoesNotTriggerHelperResolution() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());

        assertReceivedQueries(fakeUpstreamServer, List.of(
                key("com", Type.NS),
                key(DOMAIN, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(key(DOMAIN, Type.A)));
    }

    private DnsQueryKey key(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return new DnsQueryKey(Name.fromString(normalized), qtype, org.xbill.DNS.DClass.IN);
        } catch (TextParseException e) {
            fail("failed to build expected query key: " + e.getMessage());
            return null;
        }
    }
}
