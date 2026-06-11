package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final StepResolverFactory stepResolverFactory;
    private final RootHintsProvider rootHintsProvider;
    private final RecursiveCache recursiveCache;
    private final DNSSecMode dnsSecMode;

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, StepResolverFactory stepResolverFactory, DNSSecMode dnsSecMode) {
        this.stepResolverFactory = stepResolverFactory;
        this.rootHintsProvider = rootHintsProvider;
        this.recursiveCache = recursiveCache;
        this.dnsSecMode = dnsSecMode;
    }

    public RecursiveSession createSession(DnsQueryRequest request) {
        return new RecursiveSession(request, stepResolverFactory, rootHintsProvider, recursiveCache, dnsSecMode);
    }

}
