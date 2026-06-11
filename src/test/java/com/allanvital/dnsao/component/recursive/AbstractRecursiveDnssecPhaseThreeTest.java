package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.TextParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractRecursiveDnssecPhaseThreeTest extends AbstractRecursiveScenarioTest {

    protected void assertDoValues(List<Message> receivedMessages, boolean... expectedDoValues) {
        assertEquals(expectedDoValues.length, receivedMessages.size());
        for (int i = 0; i < expectedDoValues.length; i++) {
            OPTRecord opt = MessageHelper.getOpt(receivedMessages.get(i));
            assertEquals(expectedDoValues[i], MessageHelper.isDO(opt));
        }
    }

    protected DnsQueryKey key(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return new DnsQueryKey(Name.fromString(normalized), qtype, DClass.IN);
        } catch (TextParseException e) {
            fail("failed to build expected query key: " + e.getMessage());
            return null;
        }
    }

    protected List<DnsQueryKey> history(DnsQueryKey... queries) {
        return List.of(queries);
    }
}
