package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveReferralLoopFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
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
public class RecursiveReferralLoopTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "loop.allanvital.com";
    private static final String NS_HOST = "ns1.loop.com";
    private static final String NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveReferralLoopFixture fixture = new RecursiveReferralLoopFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(DOMAIN, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void stopsAfterBoundedReferralRetriesAndReturnsServfail() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertReceivedQueries(expectedQueries);
    }
}
