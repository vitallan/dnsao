package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveRootHintFailoverFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveRootHintFailoverTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FINAL_IP = "10.0.0.4";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final String FIRST_ROOT_IP = "127.0.0.20";
    private static final String SECOND_ROOT_IP = "127.0.0.21";
    private static final long REFERRAL_TTL = 300;

    private FakeServer firstRootServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        firstRootServer = startSilentFakeUdpServer();
        trackExtraFakeServer(firstRootServer);
    }

    @Override
    protected List<NameServerAddress> rootHintServers() {
        return List.of(
                new NameServerAddress(FIRST_ROOT_IP, firstRootServer.getPort()),
                new NameServerAddress(SECOND_ROOT_IP, fakeUpstreamServer.getPort())
        );
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_ROOT_IP, firstRootServer.getPort());
        stepResolverFactory.setRoute(SECOND_ROOT_IP, fakeUpstreamServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveRootHintFailoverFixture fixture = new RecursiveRootHintFailoverFixture(fakeUpstreamServer);
        expectedHistories = fixture.load(DOMAIN, FINAL_IP, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void fallsBackToSecondRootHintWhenFirstDoesNotRespond() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(firstRootServer, expectedHistories.primaryQueries());
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.secondaryQueries());
    }
}
