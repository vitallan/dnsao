package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.graph.bean.MessageHelper;
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
        Message request = MessageHelper.buildARequest(domain, true);
        Message response = MessageHelper.buildAResponse(request, responseIp, 300, false);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD), "OFF não exige AD; aceita sem AD");
        String ip = MessageHelper.extractIpFromResponseMessage(processed);
        assertEquals(this.responseIp, ip);
    }

    @Test
    void shouldNotPassThroughAD_whenUpstreamValidated() throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        Message response = MessageHelper.buildAResponse(request, responseIp, 300, true);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD));
        String ip = MessageHelper.extractIpFromResponseMessage(processed);
        assertEquals(this.responseIp, ip);
    }

    @Test
    void shouldAcceptNxdomainWithoutAD() throws Exception {
        Message request = MessageHelper.buildARequest("doesnotexist." + domain, true);
        Message nxdomain = MessageHelper.buildNxdomainResponseFrom(request, false);
        fakeDnsServer.mockResponse(request, nxdomain);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.NXDOMAIN, processed.getRcode());
        assertFalse(processed.getHeader().getFlag(Flags.AD));
    }

    @Test
    void shouldPropagateServfail() throws Exception {
        Message request = MessageHelper.buildARequest(domain, true);
        Message servfail = MessageHelper.buildServfailFrom(request);
        fakeDnsServer.mockResponse(request, servfail);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processed = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, processed.getRcode());
    }

}
