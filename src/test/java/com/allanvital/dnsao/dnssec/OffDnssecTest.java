package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OffDnssecTest extends DnssecTest {

    @BeforeEach
    public void setup() throws Exception {
        prepare(DNSSecMode.OFF);
    }

    @Test
    void shouldAcceptNoerrorWithoutAD_evenIfClientAskedDnssec() throws Exception {
        Message request = MessageUtils.buildARequest(domain, true);
        Message response = MessageUtils.buildAResponse(request, responseIp, 300, false);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD), "OFF não exige AD; aceita sem AD");
        String ip = MessageUtils.extractIpFromResponseMessage(processed);
        assertEquals(this.responseIp, ip);
    }

    @Test
    void shouldPassThroughAD_whenUpstreamValidated() throws Exception {
        Message request = MessageUtils.buildARequest(domain);
        Message response = MessageUtils.buildAResponse(request, responseIp, 300, true);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processed.getRcode());
        assertTrue(processed.getHeader().getFlag(Flags.AD));
        String ip = MessageUtils.extractIpFromResponseMessage(processed);
        assertEquals(this.responseIp, ip);
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
    void shouldPropagateServfail() throws Exception {
        Message request = MessageUtils.buildARequest(domain, true);
        Message servfail = MessageUtils.buildServfailFrom(request);
        fakeDnsServer.mockResponse(request, servfail);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, processed.getRcode());
    }

}
