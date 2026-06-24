package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveHelperAaaaFallbackFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveHelperResolutionBudgetFixture;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveServerHistories;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHelperResolutionBudgetTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FIRST_HELPER_HOST = "ns1.helper.com";
    private static final String SECOND_HELPER_HOST = "ns2.helper.com";
    private static final String SECOND_HELPER_IP = "127.0.0.1";
    private static final String HELPER_NAMESERVER_IPV6 = "2001:db8::53";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.17";
    private static final long REFERRAL_TTL = 300;
    private static final int MAX_HELPER_RESOLUTIONS = 1;
    private static final long MAX_SESSION_ELAPSED_MS = 5_000L;

    private FakeServer delegatedServer;

    @Override
    protected void beforeServerStart() throws Exception {
        assembler.setRecursiveBudgetOverrides(MAX_HELPER_RESOLUTIONS, MAX_SESSION_ELAPSED_MS);
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(HELPER_NAMESERVER_IPV6, delegatedServer.getPort());
    }

    @Test
    public void helperResolutionBudgetStopsNestedNoGlueDescentAndReturnsServfail() throws IOException {
        RecursiveHelperResolutionBudgetFixture fixture = new RecursiveHelperResolutionBudgetFixture(fakeUpstreamServer);
        RecursiveServerHistories expectedHistories = fixture.load(
                DOMAIN,
                FIRST_HELPER_HOST,
                SECOND_HELPER_HOST,
                SECOND_HELPER_IP,
                REFERRAL_TTL
        );

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());
    }

    @Test
    public void helperAThenAaaaFallbackConsumesOneBudgetSlotAndStillResolves() throws IOException {
        RecursiveHelperAaaaFallbackFixture fixture = new RecursiveHelperAaaaFallbackFixture(fakeUpstreamServer, delegatedServer);
        RecursiveServerHistories expectedHistories = fixture.load(
                DOMAIN,
                FIRST_HELPER_HOST,
                HELPER_NAMESERVER_IPV6,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IPV4,
                FINAL_IP,
                REFERRAL_TTL
        );

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());
    }
}
