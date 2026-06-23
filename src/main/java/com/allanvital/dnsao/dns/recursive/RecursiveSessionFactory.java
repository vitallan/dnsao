package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final RootHintsProvider rootHintsProvider;
    private final RecursiveCache recursiveCache;
    private final ServerRacer serverRacer;
    private final DnssecDowngradeHandler dnssecHandler;
    private final DNSSecMode dnsSecMode;
    private final RecursiveStatsCollector recursiveStatsCollector;

    public RecursiveSessionFactory(int timeoutSeconds, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, StepResolverFactory stepResolverFactory, DNSSecMode dnsSecMode, ExecutorServiceFactory executorServiceFactory, RecursiveStatsCollector recursiveStatsCollector) {
        this.rootHintsProvider = rootHintsProvider;
        this.recursiveCache = recursiveCache;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.dnssecHandler = new DnssecDowngradeHandler(dnsSecMode, recursiveStatsCollector);
        this.serverRacer = new ServerRacer(executorServiceFactory.buildCachedExecutor("dns-race"), timeoutSeconds, stepResolverFactory, dnssecHandler, recursiveStatsCollector);
        this.dnsSecMode = dnsSecMode;
    }

    public RecursiveSession createSession(DnsQueryRequest request) {
        return new RecursiveSession(request, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, recursiveStatsCollector);
    }

}
