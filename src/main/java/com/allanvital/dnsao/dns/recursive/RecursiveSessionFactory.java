package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final StepResolverFactory stepResolverFactory;
    private final RootHintsProvider rootHintsProvider;
    private final DNSSecMode dnsSecMode;

    public RecursiveSessionFactory(int timeoutMs) {
        this(timeoutMs, new RootHintsProvider(), DNSSecMode.SIMPLE);
    }

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider) {
        this(timeoutMs, rootHintsProvider, DNSSecMode.SIMPLE);
    }

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider, DNSSecMode dnsSecMode) {
        this(timeoutMs, rootHintsProvider, new StepResolverFactory(timeoutMs), dnsSecMode);
    }

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider, StepResolverFactory stepResolverFactory, DNSSecMode dnsSecMode) {
        this.stepResolverFactory = stepResolverFactory;
        this.rootHintsProvider = rootHintsProvider;
        this.dnsSecMode = dnsSecMode;
    }

    public RecursiveSession createSession(DnsQueryRequest request) {
        return new RecursiveSession(request, stepResolverFactory, rootHintsProvider, dnsSecMode);
    }

}
