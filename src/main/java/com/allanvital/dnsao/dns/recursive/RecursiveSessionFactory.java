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
    private final Integer maxNsNameResolutions;
    private final Long maxSessionElapsedMillis;

    public RecursiveSessionFactory(int timeoutSeconds, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, StepResolverFactory stepResolverFactory, DNSSecMode dnsSecMode, ExecutorServiceFactory executorServiceFactory, RecursiveStatsCollector recursiveStatsCollector) {
        this(timeoutSeconds, rootHintsProvider, recursiveCache, stepResolverFactory, dnsSecMode, executorServiceFactory, recursiveStatsCollector, null, null);
    }

    public RecursiveSessionFactory(int timeoutSeconds,
                                   RootHintsProvider rootHintsProvider,
                                   RecursiveCache recursiveCache,
                                   StepResolverFactory stepResolverFactory,
                                   DNSSecMode dnsSecMode,
                                   ExecutorServiceFactory executorServiceFactory,
                                   RecursiveStatsCollector recursiveStatsCollector,
                                   Integer maxNsNameResolutions,
                                   Long maxSessionElapsedMillis) {
        this.rootHintsProvider = rootHintsProvider;
        this.recursiveCache = recursiveCache;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.dnssecHandler = new DnssecDowngradeHandler(dnsSecMode, recursiveStatsCollector);
        this.serverRacer = new ServerRacer(executorServiceFactory.buildCachedExecutor("dns-race"), timeoutSeconds, stepResolverFactory, dnssecHandler, recursiveStatsCollector);
        this.dnsSecMode = dnsSecMode;
        this.maxNsNameResolutions = maxNsNameResolutions;
        this.maxSessionElapsedMillis = maxSessionElapsedMillis;
    }

    public RecursiveSession createSession(DnsQueryRequest request) {
        if (maxNsNameResolutions != null && maxSessionElapsedMillis != null) {
            return new RecursiveSession(request, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, recursiveStatsCollector, maxNsNameResolutions, maxSessionElapsedMillis);
        }
        return new RecursiveSession(request, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, recursiveStatsCollector);
    }

}
