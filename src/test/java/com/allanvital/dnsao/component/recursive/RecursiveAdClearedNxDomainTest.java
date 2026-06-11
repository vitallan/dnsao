package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
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
public class RecursiveAdClearedNxDomainTest extends AbstractRecursiveDnssecPhaseOneTest {

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    @BeforeEach
    public void loadScenario() {
        expectedQueries = newFixture().loadAuthenticatedNxDomain(NXDOMAIN, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void clearsAdFlagOnFinalAuthoritativeNxDomain() throws IOException {
        Message response = executeRequestOnOwnServer(NXDOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NXDOMAIN, response.getRcode());
        assertReceivedQueries(expectedQueries);
        assertAdCleared(response);
    }
}
