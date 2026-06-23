package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Rcode;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecDowngradeHandler {

    private static final List<Integer> DNSSEC_DOWNGRADE_RCODES = List.of(Rcode.REFUSED, Rcode.NOTIMP, Rcode.FORMERR);

    private final DNSSecMode dnsSecMode;
    private final RecursiveStatsCollector recursiveStatsCollector;

    public DnssecDowngradeHandler(DNSSecMode dnsSecMode) {
        this(dnsSecMode, new NoOpRecursiveStatsCollector());
    }

    public DnssecDowngradeHandler(DNSSecMode dnsSecMode, RecursiveStatsCollector recursiveStatsCollector) {
        this.dnsSecMode = dnsSecMode;
        this.recursiveStatsCollector = recursiveStatsCollector;
    }

    public StepResponse queryWithPossibleDowngrade(StepResolver resolver, StepRequest request) {
        StepResponse response = resolver.send(request);
        if (response == null) {
            return null;
        }
        if (shouldRetryWithoutDo(request, response)) {
            recursiveStatsCollector.increment(RecursiveMetric.DNSSEC_DOWNGRADE_ATTEMPTED);
            Log.DNS.trace("recursive dnssec downgrade qtype={} qname={} rcode={}", request.qtype(), request.qname(), response.toWireMessage().getRcode());
            StepRequest downgradedRequest = new StepRequest(request.qname(), request.qtype(), request.qclass(), DNSSecMode.OFF);
            StepResponse downgradedResponse = resolver.send(downgradedRequest);
            if (downgradedResponse != null) {
                recursiveStatsCollector.increment(RecursiveMetric.DNSSEC_DOWNGRADE_SUCCEEDED);
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
