package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveCnameChainFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
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
public class RecursiveCnameChainTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String INTERMEDIATE_TARGET = "alias1.com";
    private static final String FINAL_TARGET = "alias2.com";
    private static final String FINAL_IP = "10.0.0.2";
    private static final String NS_HOST = "ns1.com";
    private static final String NS_IP = "127.0.0.1";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveCnameChainFixture fixture = new RecursiveCnameChainFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(DOMAIN, INTERMEDIATE_TARGET, FINAL_TARGET, FINAL_IP, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void followsCnameChainAndReturnsFinalAddress() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(expectedQueries);
    }
}
