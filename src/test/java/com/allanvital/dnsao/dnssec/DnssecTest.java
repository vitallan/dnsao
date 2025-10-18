package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.QueryProcessor;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecTest extends TestHolder {

    protected QueryProcessor processor;
    protected String domain = "example.com";
    protected String responseIp = "10.10.10.10";

    protected void prepare(DNSSecMode dnsSecMod) throws ConfException {
        super.loadConf("1udp-upstream-nocache.yml", false);
        super.startFakeDnsServer();
        super.prepareSimpleMockResponse(domain, responseIp);
        ResolverFactory resolverFactory = new ResolverFactory(null, conf.getResolver().getUpstreams());
        QueryProcessorFactory factory = new QueryProcessorFactory(resolverFactory.getAllResolvers(), null, null, null, 1, dnsSecMod);
        processor = factory.buildQueryProcessor();
    }

    protected void shouldValidateAndReturnAD(boolean clientRequestedValidated) {
        Message request = MessageUtils.buildARequest(domain);
        if (clientRequestedValidated) {
            request = MessageUtils.buildARequest(domain, true);
        }
        Message response = MessageUtils.buildAResponse(request, responseIp, 300, true);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processedResponse = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processedResponse.getRcode());
        assertTrue(processedResponse.getHeader().getFlag(Flags.AD));

        String responseIp = MessageUtils.extractIpFromResponseMessage(processedResponse);
        Assertions.assertEquals(this.responseIp, responseIp);
    }

    @AfterEach
    public void tearDown() {
        stopFakeDnsServer();
    }

}
