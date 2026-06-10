package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;

public class RecursiveSessionFactory {

    private final StepResolverFactory stepResolverFactory;
    private final RootHintsProvider rootHintsProvider;

    public RecursiveSessionFactory(int timeoutMs) {
        this(timeoutMs, new RootHintsProvider());
    }

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider) {
        this(timeoutMs, rootHintsProvider, new StepResolverFactory(timeoutMs));
    }

    public RecursiveSessionFactory(int timeoutMs, RootHintsProvider rootHintsProvider, StepResolverFactory stepResolverFactory) {
        this.stepResolverFactory = stepResolverFactory;
        this.rootHintsProvider = rootHintsProvider;
    }

    public RecursiveSession createSession(DnsQueryRequest request) {
        return new RecursiveSession(request, stepResolverFactory, rootHintsProvider);
    }

}
