package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveNestedNoGlueFixture;
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
public class RecursiveNestedNoGlueTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String FIRST_NAMESERVER_HOST = "ns1.helper.com";
    private static final String SECOND_NAMESERVER_HOST = "ns2.helper.com";
    private static final String SECOND_NAMESERVER_IP = "127.0.0.1";
    private static final String FIRST_NAMESERVER_IP = "127.0.0.1";
    private static final String FINAL_IP = "10.0.0.9";
    private static final long REFERRAL_TTL = 300;

    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void loadScenario() {
        RecursiveNestedNoGlueFixture fixture = new RecursiveNestedNoGlueFixture(fakeUpstreamServer);
        expectedQueries = fixture.load(
                DOMAIN,
                FIRST_NAMESERVER_HOST,
                SECOND_NAMESERVER_HOST,
                SECOND_NAMESERVER_IP,
                FIRST_NAMESERVER_IP,
                FINAL_IP,
                REFERRAL_TTL
        );
    }

    @Test
    public void resolvesHelperNameThroughNestedNoGlueDelegation() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(expectedQueries);
    }
}
