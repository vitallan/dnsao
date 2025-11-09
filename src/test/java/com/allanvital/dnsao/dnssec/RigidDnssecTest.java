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
        Message request = MessageHelper.buildARequest(domain, true);
        Message response = MessageHelper.buildAResponse(request, responseIp, 300, false);
        fakeUpstreamServer.mockResponse(request, response);
        DnsQuery dnsQuery = processor.processExternalQuery(getClient(), request.toWire());
        Message processedResponse = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, processedResponse.getRcode());
    }

    @Test
    void shouldAcceptAuthenticatedNxdomain() throws Exception {
        Message request = MessageHelper.buildARequest("doesnotexist." + domain, true);
        Message nxdomainAD = MessageHelper.buildNxdomainResponseFrom(request, true);
        fakeUpstreamServer.mockResponse(request, nxdomainAD);

        DnsQuery dnsQuery = processor.processExternalQuery(getClient(), request.toWire());
        Message resp = dnsQuery.getResponse();

        assertEquals(Rcode.NXDOMAIN, resp.getRcode());
        assertFalse(resp.getHeader().getFlag(Flags.AD));
        //even when requesting DO from upstream, the AD flag must be removed because dnsao does not validate the crypt signatures
    }

    @Test
    void shouldServfailOnUnauthenticatedNxdomain() throws Exception {
        Message request = MessageHelper.buildARequest("doesnotexist." + domain, true);
        Message nxdomainNoAd = MessageHelper.buildNxdomainResponseFrom(request, false);
        fakeUpstreamServer.mockResponse(request, nxdomainNoAd);

        DnsQuery dnsQuery = processor.processExternalQuery(getClient(), request.toWire());
        Message resp = dnsQuery.getResponse();

        assertEquals(Rcode.SERVFAIL, resp.getRcode());
    }



}
