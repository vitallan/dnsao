package com.allanvital.dnsao.dns.processor;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerFacade;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerProvider;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class QueryProcessorPreHandlerResponseTest {

    @Test
    void shouldReturnPreparedPreHandlerResponse() throws Exception {
        Message refused = new Message(321);
        refused.getHeader().setFlag(Flags.QR);
        refused.getHeader().setRcode(Rcode.REFUSED);

        PreHandlerFacade preHandlerFacade = new PreHandlerFacade(new PreHandlerProvider(DNSSecMode.OFF)) {
            @Override
            public DnsQueryRequest prepare(InetAddress clientAddress, Message request, boolean isInternalQuery) throws PreHandlerException {
                throw new PreHandlerException(refused, "blocked by policy");
            }
        };

        QueryProcessor processor = new QueryProcessor(new QueryProcessorDependencies(preHandlerFacade, null, null));

        DnsQuery dnsQuery = processor.processQuery(InetAddress.getByName("192.168.1.20"), new Message(321), false);

        assertNotNull(dnsQuery);
        assertNotNull(dnsQuery.getResponse());
        assertEquals(Rcode.REFUSED, dnsQuery.getResponse().getRcode());
        assertEquals(321, dnsQuery.getResponse().getHeader().getID());
        assertTrue(dnsQuery.getResponse().getHeader().getFlag(Flags.QR));
    }
}
