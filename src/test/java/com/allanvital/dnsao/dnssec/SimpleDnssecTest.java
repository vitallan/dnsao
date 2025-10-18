package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleDnssecTest extends DnssecTest {

    @BeforeEach
    public void setup() throws Exception {
        prepare(DNSSecMode.SIMPLE);
    }

    @Test
    void shouldReturnADEvenIfClientDidNotAskForDnssec() throws Exception {
        shouldValidateAndReturnAD(false);
    }

    @Test
    void shouldReturnADIfClientAskForDnssec() throws Exception {
        shouldValidateAndReturnAD(true);
    }

    @Test
    void simpleShouldAcceptNoerrorWithoutADAndPropagateWithoutAD() throws Exception {
        Message request = MessageUtils.buildARequest(domain, true);

        Message response = MessageUtils.buildAResponse(request, responseIp, 300, false);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD));
        String ip = MessageUtils.extractIpFromResponseMessage(processed);
        Assertions.assertEquals(this.responseIp, ip);
    }

    @Test
    void shouldAcceptNxdomainWithoutAD() throws Exception {
        Message request = MessageUtils.buildARequest("doesnotexist." + domain, true);

        Message nxdomain = MessageUtils.buildNxdomainResponseFrom(request, false);
        fakeDnsServer.mockResponse(request, nxdomain);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NXDOMAIN, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD));
    }

    @Test
    void shouldReturnSERVFAILWhenUpstreamSignalsBogus() throws Exception {
        Message request = MessageUtils.buildARequest(domain, true);
        Message servfail = MessageUtils.buildServfailFrom(request);
        fakeDnsServer.mockResponse(request, servfail);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, processed.getRcode());
    }

}
