package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.cache.rewarm.RewarmWorker;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.exc.ConfException;

import java.util.concurrent.ExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SystemGraphAssembler {

    private final OverrideRegistry overrideRegistry = new OverrideRegistry();
    private final QueryInfraAssembler queryInfraAssembler;
    private QueryProcessorDependencies queryProcessorDependencies;

    public SystemGraphAssembler() {
        queryInfraAssembler = new QueryInfraAssembler(overrideRegistry);
    }

    public DnsServer assemble(Conf conf) throws ConfException {
        CacheConf cacheConf = conf.getCache();
        ResolverConf resolverConf = conf.getResolver();
        ServerConf serverConf = conf.getServer();
        MiscConf miscConf = conf.getMisc();
        ExecutorServiceFactory executorServiceFactory = executorServiceFactory();

        FixedTimeRewarmScheduler fixedTimeRewarmScheduler = rewarmScheduler(20_000);
        CacheManager cacheManager = cacheManager(cacheConf, fixedTimeRewarmScheduler, miscConf.getExpiredConf());

        queryProcessorDependencies = queryProcessorDependencies(executorServiceFactory, resolverConf, miscConf, cacheManager);

        QueryProcessorFactory factory = queryProcessorFactory(queryProcessorDependencies);

        scheduleRewarmWorker(executorServiceFactory, cacheConf, fixedTimeRewarmScheduler, cacheManager, factory);
        return dnsServer(serverConf, factory, executorServiceFactory);
    }

    public QueryProcessorDependencies queryProcessorDependencies(ExecutorServiceFactory executorServiceFactory, ResolverConf resolverConf, MiscConf miscConf, CacheManager cacheManager) throws ConfException {
        return queryInfraAssembler.assemble(resolverConf, miscConf, cacheManager, executorServiceFactory);
    }

    public OverrideRegistry getOverrideRegistry() {
        return this.overrideRegistry;
    }

    private DnsServer dnsServer(ServerConf conf, QueryProcessorFactory queryProcessorFactory, ExecutorServiceFactory executorServiceFactory) {
        return new DnsServer(conf, queryProcessorFactory, executorServiceFactory);
    }

    private static RewarmWorker scheduleRewarmWorker(ExecutorServiceFactory executorServiceFactory, CacheConf cacheConf, FixedTimeRewarmScheduler fixedTimeRewarmScheduler, CacheManager cacheManager, QueryProcessorFactory queryProcessorFactory) {
        if (cacheConf.isRewarm()) {
            ExecutorService rewarmExecutorService = executorServiceFactory.buildExecutor("rewarm", 1);
            RewarmWorker rewarmWorker = new RewarmWorker(fixedTimeRewarmScheduler, cacheManager, queryProcessorFactory, cacheConf.getMaxRewarmCount());
            rewarmExecutorService.submit(rewarmWorker);
            return rewarmWorker;
        }
        return null;
    }

    CacheManager cacheManager(CacheConf cacheConf, FixedTimeRewarmScheduler fixedTimeRewarmScheduler, ExpiredConf expiredConf) {
        return overrideRegistry.getRegisteredModule(CacheManager.class)
                .orElse(new CacheManager(cacheConf, fixedTimeRewarmScheduler, expiredConf));
    }

    private FixedTimeRewarmScheduler rewarmScheduler(long timeBeforeTtlToTriggerRewarm) {
        return overrideRegistry.getRegisteredModule(FixedTimeRewarmScheduler.class)
                .orElse(new FixedTimeRewarmScheduler(timeBeforeTtlToTriggerRewarm));
    }

    QueryProcessorFactory queryProcessorFactory(QueryProcessorDependencies queryProcessorDependencies) {
        return new QueryProcessorFactory(queryProcessorDependencies);
    }

    ExecutorServiceFactory executorServiceFactory() {
        return overrideRegistry.getRegisteredModule(ExecutorServiceFactory.class)
                .orElse(new ExecutorServiceFactory());
    }

}
