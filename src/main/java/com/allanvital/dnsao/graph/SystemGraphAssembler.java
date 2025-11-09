package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepKickstarter;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.cache.rewarm.RewarmWorker;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.web.StatsCollector;

import java.util.concurrent.ExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SystemGraphAssembler {

    protected final OverrideRegistry overrideRegistry = new OverrideRegistry();
    private QueryInfraAssembler queryInfraAssembler;

    public DnsServer assemble(Conf conf) throws ConfException {
        queryInfraAssembler = queryInfraAssembler();
        CacheConf cacheConf = conf.getCache();
        ResolverConf resolverConf = conf.getResolver();
        ServerConf serverConf = conf.getServer();
        MiscConf miscConf = conf.getMisc();
        ExecutorServiceFactory executorServiceFactory = executorServiceFactory();

        FixedTimeRewarmScheduler fixedTimeRewarmScheduler = rewarmScheduler(cacheConf.getSecsBeforeTtlToRewarm());
        CacheManager cacheManager = cacheManager(cacheConf, fixedTimeRewarmScheduler, miscConf.getExpiredConf());

        NotificationManager notificationManager = notificationManager(miscConf.isQueryLog());
        StatsCollector statsCollector = statsCollector(notificationManager);

        QueryProcessorDependencies queryProcessorDependencies = queryProcessorDependencies(executorServiceFactory, resolverConf, miscConf, cacheManager, notificationManager);

        QueryProcessorFactory factory = queryProcessorFactory(queryProcessorDependencies);

        KeepProvider keepProvider = keepProvider(cacheConf);
        KeepKickstarter kickstarter = keepKickStarter(keepProvider, factory);
        scheduleRewarmWorker(executorServiceFactory, cacheConf, fixedTimeRewarmScheduler, cacheManager, factory, keepProvider);
        kickstarter.kickStartKeep();

        return dnsServer(serverConf, factory, executorServiceFactory, statsCollector);
    }

    KeepKickstarter keepKickStarter(KeepProvider keepProvider, QueryProcessorFactory queryProcessorFactory) {
        return new KeepKickstarter(keepProvider, queryProcessorFactory);
    }

    KeepProvider keepProvider(CacheConf cacheConf) {
        return new KeepProvider(cacheConf);
    }

    public QueryProcessorDependencies queryProcessorDependencies(ExecutorServiceFactory executorServiceFactory, ResolverConf resolverConf, MiscConf miscConf, CacheManager cacheManager, NotificationManager notificationManager) throws ConfException {
        return queryInfraAssembler.assemble(resolverConf, miscConf, cacheManager, executorServiceFactory, notificationManager);
    }

    public <T> void registerOverride(T module) {
        this.overrideRegistry.registerOverride(module);
    }

    private DnsServer dnsServer(ServerConf conf, QueryProcessorFactory queryProcessorFactory, ExecutorServiceFactory executorServiceFactory, StatsCollector statsCollector) {
        return new DnsServer(conf, queryProcessorFactory, executorServiceFactory, statsCollector);
    }

    private static RewarmWorker scheduleRewarmWorker(ExecutorServiceFactory executorServiceFactory,
                                                     CacheConf cacheConf,
                                                     FixedTimeRewarmScheduler fixedTimeRewarmScheduler,
                                                     CacheManager cacheManager,
                                                     QueryProcessorFactory queryProcessorFactory,
                                                     KeepProvider keepProvider) {
        if (cacheConf.isRewarm()) {
            ExecutorService rewarmExecutorService = executorServiceFactory.buildExecutor("rewarm", 1);
            RewarmWorker rewarmWorker = new RewarmWorker(fixedTimeRewarmScheduler, cacheManager, queryProcessorFactory, keepProvider, cacheConf.getMaxRewarmCount());
            rewarmExecutorService.submit(rewarmWorker);
            return rewarmWorker;
        }
        return null;
    }

    QueryInfraAssembler queryInfraAssembler() {
        return new QueryInfraAssembler(overrideRegistry);
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

    NotificationManager notificationManager(boolean queryLogEnabled) {
        return new NotificationManager(queryLogEnabled);
    }

    StatsCollector statsCollector(NotificationManager notificationManager) {
        StatsCollector statsCollector = new StatsCollector();
        notificationManager.querySubscribe(statsCollector);
        return statsCollector;
    }

}
