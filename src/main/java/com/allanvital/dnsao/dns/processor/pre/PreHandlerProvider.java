package com.allanvital.dnsao.dns.processor.pre;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.pre.handler.*;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PreHandlerProvider {

    private final List<PreHandler> orderedPreHandlers = new LinkedList<>();

    public PreHandlerProvider(DNSSecMode dnsSecMode) {
        orderedPreHandlers.add(new ValidQueryFilter());
        orderedPreHandlers.add(new IdSwapper());
        orderedPreHandlers.add(new HeaderCleaner(dnsSecMode));
        orderedPreHandlers.add(new OptCleaner());
        orderedPreHandlers.add(new DnsPrivacyShaper(dnsSecMode));
        orderedPreHandlers.add(new RequestLogger());
    }

    public List<PreHandler> getOrderedPreHandlers() {
        return orderedPreHandlers;
    }

}
