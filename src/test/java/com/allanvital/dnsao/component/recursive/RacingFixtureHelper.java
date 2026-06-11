package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.AbstractRecursiveScenarioFixture;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import static org.junit.jupiter.api.Assertions.fail;

class RacingFixtureHelper extends AbstractRecursiveScenarioFixture {

    RacingFixtureHelper(FakeServer fakeServer) {
        super(fakeServer);
    }

    @Override
    public Message buildRequest(String qname, int qtype) {
        return super.buildRequest(qname, qtype);
    }

    @Override
    public Message buildNsReferralWithGlueResponse(Message request, String nsHost, String nsIp, long ttl) {
        return super.buildNsReferralWithGlueResponse(request, nsHost, nsIp, ttl);
    }

    @Override
    public void clearHistory() {
        super.clearHistory();
    }

    static Message buildQuery(String domain, int qtype) {
        try {
            String fqdn = domain.endsWith(".") ? domain : domain + ".";
            return Message.newQuery(Record.newRecord(Name.fromString(fqdn), qtype, DClass.IN));
        } catch (TextParseException e) {
            fail("failed to build query: " + e.getMessage());
            return null;
        }
    }
}
