package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.graph.bean.MessageHelper;
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
public class RecursiveDnssecRigidDoTest extends AbstractRecursiveDnssecPhaseOneTest {

    @Override
    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.RIGID;
    }

    @BeforeEach
    public void loadScenario() {
        expectedQueries = newFixture().loadHappyPathWithGlue(DOMAIN, FINAL_IP, NS_HOST, NS_IP, REFERRAL_TTL);
    }

    @Test
    public void recursiveStepsIncludeDoInRigidMode() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(expectedQueries);
        assertRecursiveStepQueriesCarryOptAndDo(true);
    }
}
