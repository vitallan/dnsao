package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.xbill.DNS.Rcode;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecDowngradeHandler {

    private static final List<Integer> DNSSEC_DOWNGRADE_RCODES = List.of(Rcode.REFUSED, Rcode.NOTIMP, Rcode.FORMERR);

    private final DNSSecMode dnsSecMode;

    public DnssecDowngradeHandler(DNSSecMode dnsSecMode) {
        this.dnsSecMode = dnsSecMode;
    }

    public StepResponse queryWithPossibleDowngrade(StepResolver resolver, StepRequest request) {
        StepResponse response = resolver.send(request);
        if (response == null) {
            return null;
        }
        if (shouldRetryWithoutDo(request, response)) {
            StepRequest downgradedRequest = new StepRequest(request.qname(), request.qtype(), request.qclass(), DNSSecMode.OFF);
            StepResponse downgradedResponse = resolver.send(downgradedRequest);
            if (downgradedResponse != null) {
                return downgradedResponse;
            }
        }
        return response;
    }

    private boolean shouldRetryWithoutDo(StepRequest request, StepResponse response) {
        if (dnsSecMode != DNSSecMode.SIMPLE || !request.dnssecEnabled()) {
            return false;
        }
        for (Integer retryRcode : DNSSEC_DOWNGRADE_RCODES) {
            if (response.isRcode(retryRcode)) {
                return true;
            }
        }
        return false;
    }

}
