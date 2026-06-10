package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveHappyPathWithoutGlueFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHappyPathWithoutGlueTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FINAL_IP = "5.6.7.8";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveHappyPathWithoutGlueFixture fixture = new RecursiveHappyPathWithoutGlueFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(DOMAIN, NS_HOST, NS_IP, FINAL_IP, REFERRAL_TTL);
    }

    @Test
    public void answersExpectedRecordThroughMinifiedNoGluePath() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertTrue(response.getHeader().getFlag(Flags.RA));
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(expectedQueries);
    }
}
