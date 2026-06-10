package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveNameserverFailoverFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveNameserverFailoverTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FIRST_NS_HOST = "ns1.com";
    private static final String FIRST_NS_IP = "127.0.0.10";
    private static final String SECOND_NS_HOST = "ns2.com";
    private static final String SECOND_NS_IP = "127.0.0.11";
    private static final String FINAL_IP = "10.0.0.3";
    private static final long REFERRAL_TTL = 300;

    private FakeServer firstNameserver;
    private FakeServer secondNameserver;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        firstNameserver = startSilentFakeUdpServer();
        trackExtraFakeServer(firstNameserver);
        secondNameserver = startFakeUdpServer();
        trackExtraFakeServer(secondNameserver);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(FIRST_NS_IP, firstNameserver.getPort());
        stepResolverFactory.setRoute(SECOND_NS_IP, secondNameserver.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveNameserverFailoverFixture fixture = new RecursiveNameserverFailoverFixture(fakeUpstreamServer, secondNameserver);
        expectedHistories = fixture.load(DOMAIN, FIRST_NS_HOST, FIRST_NS_IP, SECOND_NS_HOST, SECOND_NS_IP, FINAL_IP, REFERRAL_TTL);
    }

    @Test
    public void fallsBackToSecondNameserverWhenFirstDoesNotRespond() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(firstNameserver, expectedHistories.secondaryQueries());
        assertReceivedQueries(secondNameserver, expectedHistories.tertiaryQueries());
    }
}
