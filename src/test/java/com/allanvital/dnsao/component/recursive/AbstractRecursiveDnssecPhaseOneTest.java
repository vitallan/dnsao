package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.RecursiveDnssecPhaseOneFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractRecursiveDnssecPhaseOneTest extends AbstractRecursiveScenarioTest {

    protected static final String DOMAIN = "allanvital.com";
    protected static final String NXDOMAIN = "doesnotexist.allanvital.com";
    protected static final String FINAL_IP = "10.0.0.21";
    protected static final String NS_HOST = "ns1.com";
    protected static final String NS_IP = "127.0.0.1";
    protected static final long REFERRAL_TTL = 300;

    protected List<DnsQueryKey> expectedQueries;

    protected RecursiveDnssecPhaseOneFixture newFixture() {
        return new RecursiveDnssecPhaseOneFixture(fakeUpstreamServer);
    }

    protected void assertRecursiveStepQueriesCarryOptAndDo(boolean expectedDo) {
        List<Message> receivedMessages = getReceivedMessages(fakeUpstreamServer);
        assertEquals(expectedQueries.size(), receivedMessages.size());
        for (Message receivedMessage : receivedMessages) {
            OPTRecord opt = MessageHelper.getOpt(receivedMessage);
            assertEquals(expectedDo, MessageHelper.isDO(opt));
        }
    }

    protected void assertAdCleared(Message response) {
        assertFalse(response.getHeader().getFlag(Flags.AD));
    }
}
