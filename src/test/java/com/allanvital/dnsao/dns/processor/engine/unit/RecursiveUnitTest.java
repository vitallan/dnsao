package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

public class RecursiveUnitTest {

    private final RecursiveUnit unit = new RecursiveUnit();

    @Test
    public void respondsToStubDomain() throws Exception {
        Message request = buildARequest("allanvital.com");
        DnsQueryRequest queryRequest = buildQueryRequest(request);

        DnsQueryResponse response = unit.innerProcess(queryRequest);

        assertNotNull(response);
        Message message = response.getResponse();
        assertEquals(Rcode.NOERROR, message.getRcode());
        assertTrue(message.getHeader().getFlag(Flags.QR));
        assertTrue(message.getHeader().getFlag(Flags.RA));

        org.xbill.DNS.Record question = message.getQuestion();
        assertNotNull(question);
        assertEquals(Name.fromString("allanvital.com."), question.getName());
        assertEquals(Type.A, question.getType());

        org.xbill.DNS.Record[] answers = message.getSectionArray(Section.ANSWER);
        assertEquals(1, answers.length);
        assertInstanceOf(ARecord.class, answers[0]);
        ARecord a = (ARecord) answers[0];
        assertEquals(InetAddress.getByName("192.0.2.1"), a.getAddress());
        assertEquals(60, a.getTTL());
    }

    @Test
    public void copiesRdBitBack() throws Exception {
        Message request = buildARequest("allanvital.com");
        request.getHeader().setFlag(Flags.RD);
        DnsQueryRequest queryRequest = buildQueryRequest(request);

        DnsQueryResponse response = unit.innerProcess(queryRequest);

        assertNotNull(response);
        assertTrue(response.getResponse().getHeader().getFlag(Flags.RD));
    }

    @Test
    public void returnsNullForNonMatchingDomain() throws Exception {
        Message request = buildARequest("example.com");
        DnsQueryRequest queryRequest = buildQueryRequest(request);

        DnsQueryResponse response = unit.innerProcess(queryRequest);

        assertNull(response);
    }

    @Test
    public void returnsNullForNonAType() throws Exception {
        Message request = Message.newQuery(org.xbill.DNS.Record.newRecord(Name.fromString("allanvital.com."), Type.AAAA, DClass.IN));
        DnsQueryRequest queryRequest = new DnsQueryRequest(InetAddress.getLoopbackAddress());
        queryRequest.setRequest(request);
        queryRequest.setOriginalRequest(request);

        DnsQueryResponse response = unit.innerProcess(queryRequest);

        assertNull(response);
    }

    @Test
    public void returnsRecursionResolvedBy() {
        assertEquals(QueryResolvedBy.RECURSION, unit.unitResolvedBy());
    }

    private static DnsQueryRequest buildQueryRequest(Message request) {
        DnsQueryRequest queryRequest = new DnsQueryRequest(InetAddress.getLoopbackAddress());
        queryRequest.setRequest(request);
        queryRequest.setOriginalRequest(request);
        return queryRequest;
    }

    private static Message buildARequest(String domain) {
        try {
            return Message.newQuery(org.xbill.DNS.Record.newRecord(Name.fromString(domain + "."), Type.A, DClass.IN));
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

}
