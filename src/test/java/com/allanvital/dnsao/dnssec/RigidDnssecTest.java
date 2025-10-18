package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RigidDnssecTest extends DnssecTest {

    @BeforeEach
    public void setup() throws Exception {
        prepare(DNSSecMode.RIGID);
    }

    @Test
    public void shouldValidateAndReturnADevenIfClientDidNotAskForDnssec() throws Exception {
        shouldValidateAndReturnAD(false);
    }

    @Test
    void shouldHonorClientDOAndReturnADWhenUpstreamValidated() throws Exception {
        shouldValidateAndReturnAD(true);
    }

    @Test
    void shouldReturnSERVFAILWhenUpstreamDoesNotProvideDnssecOrValidation() throws Exception {
        Message request = MessageUtils.buildARequest(domain, true);
        Message response = MessageUtils.buildAResponse(request, responseIp, 300, false);
        fakeDnsServer.mockResponse(request, response);
        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processedResponse = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, processedResponse.getRcode());
    }

    @Test
    void shouldAcceptAuthenticatedNxdomain() throws Exception {
        Message request = MessageUtils.buildARequest("doesnotexist." + domain, true);
        Message nxdomainAD = MessageUtils.buildNxdomainResponseFrom(request, true);
        fakeDnsServer.mockResponse(request, nxdomainAD);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message resp = dnsQuery.getResponse();

        assertEquals(Rcode.NXDOMAIN, resp.getRcode());
        assertTrue(resp.getHeader().getFlag(Flags.AD));
    }

    @Test
    void shouldServfailOnUnauthenticatedNxdomain() throws Exception {
        Message request = MessageUtils.buildARequest("doesnotexist." + domain, true);
        Message nxdomainNoAd = MessageUtils.buildNxdomainResponseFrom(request, false);
        fakeDnsServer.mockResponse(request, nxdomainNoAd);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message resp = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, resp.getRcode());
    }



}
