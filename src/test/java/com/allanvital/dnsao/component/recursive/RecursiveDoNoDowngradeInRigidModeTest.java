package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.component.fixture.recursive.RecursiveDoDowngradeFixture;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveDoNoDowngradeInRigidModeTest extends AbstractRecursiveDnssecPhaseThreeTest {

    private static final String DOMAIN = "phase3-rigid.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IPV4 = "127.0.0.1";
    private static final String TRANSPORT_NAMESERVER_HOST = "ns1.phase3-rigid.com";
    private static final String TRANSPORT_NAMESERVER_IP = "127.0.0.96";
    private static final String FINAL_IP = "10.0.0.25";
    private static final long REFERRAL_TTL = 300;

    private FakeServer transportServer;

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.RIGID;
    }

    @Override
    protected void beforeServerStart() throws Exception {
        transportServer = startFakeUdpServer();
        trackExtraFakeServer(transportServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(TRANSPORT_NAMESERVER_IP, transportServer.getPort());
    }

    @BeforeEach
    public void loadScenario() {
        RecursiveDoDowngradeFixture fixture = new RecursiveDoDowngradeFixture(fakeUpstreamServer, transportServer);
        fixture.load(
                DOMAIN,
                BOOTSTRAP_NS_HOST,
                BOOTSTRAP_NS_IPV4,
                TRANSPORT_NAMESERVER_HOST,
                TRANSPORT_NAMESERVER_IP,
                FINAL_IP,
                REFERRAL_TTL,
                Rcode.REFUSED
        );
    }

    @Test
    public void doesNotRetryWithoutDoInRigidMode() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertReceivedQueries(fakeUpstreamServer, history(key("com", Type.NS), key(DOMAIN, Type.NS)));
        assertReceivedQueries(transportServer, history(key(DOMAIN, Type.A)));
        assertDoValues(getReceivedMessages(fakeUpstreamServer), true, true);
        assertDoValues(getReceivedMessages(transportServer), true);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
    }
}
