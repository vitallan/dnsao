package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecTest extends TestHolder {

    protected QueryProcessor processor;
    protected String domain = "example.com";
    protected String responseIp = "10.10.10.10";

    protected void prepare(DNSSecMode dnsSecMod) throws ConfException {
        loadConf("1udp-upstream-nocache.yml");
        conf.getMisc().setDnssec(dnsSecMod.name());
        safeStartWithPresetConf();
        super.prepareSimpleMockResponse(domain, responseIp);
        processor = assembler.getQueryProcessorFactory().buildQueryProcessor();
    }

    protected void shouldValidateAndReturnAD(boolean clientRequestedValidated) {
        Message request = MessageHelper.buildARequest(domain);
        if (clientRequestedValidated) {
            request = MessageHelper.buildARequest(domain, true);
        }
        Message response = MessageHelper.buildAResponse(request, responseIp, 300, true);
        fakeDnsServer.mockResponse(request, response);

        DnsQuery dnsQuery = processor.processQuery(getClient(), request.toWire());
        Message processedResponse = dnsQuery.getResponse();

        assertEquals(Rcode.NOERROR, processedResponse.getRcode());
        assertFalse(processedResponse.getHeader().getFlag(Flags.AD));

        String responseIp = MessageHelper.extractIpFromResponseMessage(processedResponse);
        Assertions.assertEquals(this.responseIp, responseIp);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
