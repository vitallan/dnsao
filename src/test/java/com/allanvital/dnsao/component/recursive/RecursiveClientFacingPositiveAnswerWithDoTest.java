package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveClientFacingPositiveAnswerFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveClientFacingPositiveAnswerWithDoTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "client-facing-do.com";
    private static final String DELEGATED_NAMESERVER_HOST = "ns1.client-facing-do.com";
    private static final String DELEGATED_NAMESERVER_IP = "127.0.0.112";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.112";
    private static final long TTL = 300;

    private FakeServer delegatedServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(DELEGATED_NAMESERVER_IP, delegatedServer.getPort());
    }

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveClientFacingPositiveAnswerFixture fixture = new RecursiveClientFacingPositiveAnswerFixture(fakeUpstreamServer, delegatedServer);
        expectedHistories = fixture.loadAuthoritativeStylePositiveAnswer(
                DOMAIN,
                DELEGATED_NAMESERVER_HOST,
                DELEGATED_NAMESERVER_IP,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                FINAL_IP,
                TTL,
                true
        );
    }

    @Test
    public void preservesAnswerRrsigWhileClearingAuthoritativeShape() throws IOException {
        Message response = executeRequestOnOwnServer(MessageHelper.buildARequest(DOMAIN, true));

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertFalse(response.getHeader().getFlag(Flags.AA));
        assertEquals(0, response.getSection(Section.AUTHORITY).size());
        assertEquals(1, response.getSection(Section.ANSWER).stream().filter(r -> r.getType() == Type.A).count());
        assertTrue(response.getSection(Section.ANSWER).stream().anyMatch(r -> r.getType() == Type.RRSIG));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());
    }
}
