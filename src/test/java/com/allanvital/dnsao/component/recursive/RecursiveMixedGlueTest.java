package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveMixedGlueFixture;
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
public class RecursiveMixedGlueTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "mixed-glue.com";
    private static final String GLUED_NAMESERVER_HOST = "ns1.mix.com";
    private static final String GLUED_NAMESERVER_IP = "127.0.0.60";
    private static final String GLUE_LESS_NAMESERVER_HOST = "ns2.mix.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.13";
    private static final long REFERRAL_TTL = 300;

    private FakeServer delegatedServer;
    private RecursiveServerHistories expectedHistories;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = startFakeUdpServer();
        trackExtraFakeServer(delegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(GLUED_NAMESERVER_IP, delegatedServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveMixedGlueFixture fixture = new RecursiveMixedGlueFixture(fakeUpstreamServer, delegatedServer);
        expectedHistories = fixture.load(
                DOMAIN,
                GLUED_NAMESERVER_HOST,
                GLUED_NAMESERVER_IP,
                GLUE_LESS_NAMESERVER_HOST,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IP,
                FINAL_IP,
                REFERRAL_TTL
        );
    }

    @Test
    public void proceedsUsingAvailableGlueWhenDelegationHasMixedGlue() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, expectedHistories.primaryQueries());
        assertReceivedQueries(delegatedServer, expectedHistories.secondaryQueries());
    }
}
