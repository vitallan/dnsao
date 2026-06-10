package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveHappyPathWithGlueFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHappyPathWithGlueTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String DOMAIN_DOT = DOMAIN + ".";
    private static final String FINAL_IP = "192.0.2.1";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveHappyPathWithGlueFixture fixture = new RecursiveHappyPathWithGlueFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(DOMAIN, FINAL_IP, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void answersExpectedRecordThroughMinifiedGluePath() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertTrue(response.getHeader().getFlag(Flags.RA));
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));

        Record question = response.getQuestion();
        assertNotNull(question);
        assertEquals(Name.fromString(DOMAIN_DOT), question.getName());

        assertReceivedQueries(expectedQueries);
    }
}
