package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveEmptyReferralFixture;
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
public class RecursiveEmptyReferralTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "empty-referral.com";
    private static final String EMPTY_NAMESERVER_HOST = "ns1.empty.com";
    private static final String BOOTSTRAP_NS_HOST = "ns-root.com";
    private static final String BOOTSTRAP_NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveEmptyReferralFixture fixture = new RecursiveEmptyReferralFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(DOMAIN, EMPTY_NAMESERVER_HOST, BOOTSTRAP_NS_HOST, BOOTSTRAP_NS_IP, REFERRAL_TTL);
    }

    @Test
    public void failsCleanlyWhenDelegationHasNoUsableFollowUp() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertReceivedQueries(expectedQueries);
    }
}
