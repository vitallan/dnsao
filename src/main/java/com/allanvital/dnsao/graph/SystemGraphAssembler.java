package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.CacheScavenger;
import com.allanvital.dnsao.cache.keep.KeepKickstarter;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.cache.rewarm.RewarmCoordinator;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.MutableState;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.UpstreamThreadPoolExecutor;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.infra.dir.TempDir;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.web.json.JsonBuilder;
import com.allanvital.dnsao.web.stats.StatsCollector;
import com.allanvital.dnsao.web.stats.db.DbStatsCollector;
import com.allanvital.dnsao.web.stats.memory.MemoryStatsCollector;

import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.Constants.DB_DEFAULT_NAME;

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
        MutableState mutableState = createMutableState(miscConf.isBlockingEnabled());
        ExecutorServiceFactory executorServiceFactory = executorServiceFactory();

        FixedTimeRewarmScheduler fixedTimeRewarmScheduler = rewarmScheduler(cacheConf.getSecsBeforeTtlToRewarm());
        KeepProvider keepProvider = keepProvider(cacheConf);
        CacheManager cacheManager = cacheManager(cacheConf, fixedTimeRewarmScheduler, miscConf.getExpiredConf(), keepProvider);

        NotificationManager notificationManager = notificationManager(miscConf.isQueryLog());
        StatsCollector statsCollector = statsCollector(serverConf, notificationManager);

        UpstreamThreadPoolExecutor upstreamThreadPoolExecutor = new UpstreamThreadPoolExecutor(
                executorServiceFactory,
                resolverConf.getUpstreamThreadPoolSize(),
                resolverConf.getUpstreamQueueSize()
        );

        QueryProcessorDependencies queryProcessorDependencies = queryProcessorDependencies(executorServiceFactory, upstreamThreadPoolExecutor, conf, cacheManager, notificationManager, mutableState);

        QueryProcessorFactory factory = queryProcessorFactory(queryProcessorDependencies);

        KeepKickstarter kickstarter = keepKickStarter(keepProvider, factory);
        scheduleRewarmWorker(executorServiceFactory, cacheConf, fixedTimeRewarmScheduler, cacheManager, factory);
        scheduleScavenger(executorServiceFactory, cacheConf, cacheManager);
        kickstarter.kickStartKeep();

        JsonBuilder jsonBuilder = new JsonBuilder(statsCollector, cacheManager.getCacheStats());

        return dnsServer(serverConf, factory, executorServiceFactory, statsCollector, upstreamThreadPoolExecutor, jsonBuilder, mutableState);
    }

    KeepKickstarter keepKickStarter(KeepProvider keepProvider, QueryProcessorFactory queryProcessorFactory) {
        return new KeepKickstarter(keepProvider, queryProcessorFactory);
    }

    KeepProvider keepProvider(CacheConf cacheConf) {
        return new KeepProvider(cacheConf);
    }

    public QueryProcessorDependencies queryProcessorDependencies(ExecutorServiceFactory executorServiceFactory,
                                                                  UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                                                                  Conf conf,
                                                                  CacheManager cacheManager,
                                                                  NotificationManager notificationManager,
                                                                  MutableState mutableState) throws ConfException {
        return queryInfraAssembler.assemble(conf, cacheManager, executorServiceFactory, upstreamThreadPoolExecutor, notificationManager, mutableState);
    }

    public <T> void registerOverride(T module) {
        this.overrideRegistry.registerOverride(module);
    }

    private DnsServer dnsServer(ServerConf conf,
                                QueryProcessorFactory queryProcessorFactory,
                                ExecutorServiceFactory executorServiceFactory,
                                StatsCollector statsCollector,
                                UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                                JsonBuilder jsonBuilder,
                                MutableState mutableState) {
        return new DnsServer(conf, queryProcessorFactory, executorServiceFactory, statsCollector, upstreamThreadPoolExecutor, jsonBuilder, mutableState, conf.getAuthPass());
    }

    private static RewarmCoordinator scheduleRewarmWorker(ExecutorServiceFactory executorServiceFactory,
                                                          CacheConf cacheConf,
                                                          FixedTimeRewarmScheduler fixedTimeRewarmScheduler,
                                                          CacheManager cacheManager,
                                                          QueryProcessorFactory queryProcessorFactory) {
        if (cacheConf.isRewarm()) {
            ExecutorService coordinatorExecutorService = executorServiceFactory.buildExecutor("rewarm-coordinator", 1);
            ExecutorService rewarmExecutorService = executorServiceFactory.buildExecutor("rewarm-exec", cacheConf.getRewarmWorkerPoolSize());
            RewarmCoordinator rewarmCoordinator = new RewarmCoordinator(
                    fixedTimeRewarmScheduler,
                    cacheManager,
                    queryProcessorFactory,
                    cacheConf.getMaxRewarmCount(),
                    rewarmExecutorService,
                    cacheConf.getRewarmWorkerPoolSize()
            );
            coordinatorExecutorService.submit(rewarmCoordinator);
            return rewarmCoordinator;
        }
        return null;
    }

    private void scheduleScavenger(ExecutorServiceFactory executorServiceFactory,
                                    CacheConf cacheConf,
                                    CacheManager cacheManager) {
        if (cacheConf.isEnabled()) {
            ScheduledExecutorService scavengerExecutor = executorServiceFactory.buildScheduledExecutor("scavenger");
            scavengerExecutor.scheduleAtFixedRate(new CacheScavenger(cacheManager), 0, 500, TimeUnit.MILLISECONDS);
        }
    }

    QueryInfraAssembler queryInfraAssembler() {
        return new QueryInfraAssembler(overrideRegistry);
    }

    CacheManager cacheManager(CacheConf cacheConf,
                              FixedTimeRewarmScheduler fixedTimeRewarmScheduler,
                              ExpiredConf expiredConf,
                              KeepProvider keepProvider) {
        return overrideRegistry.getRegisteredModule(CacheManager.class)
                .orElse(new CacheManager(cacheConf, fixedTimeRewarmScheduler, expiredConf, keepProvider));
    }

    private FixedTimeRewarmScheduler rewarmScheduler(long timeBeforeTtlToTriggerRewarm) {
        return overrideRegistry.getRegisteredModule(FixedTimeRewarmScheduler.class)
                .orElse(FixedTimeRewarmScheduler.fromSeconds(timeBeforeTtlToTriggerRewarm));
    }

    protected MutableState createMutableState(boolean blockingEnabled) {
        return new MutableState(blockingEnabled);
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

    MemoryStatsCollector memoryStatsCollector(NotificationManager notificationManager) {
        MemoryStatsCollector memoryStatsCollector = new MemoryStatsCollector();
        notificationManager.querySubscribe(memoryStatsCollector);
        return memoryStatsCollector;
    }

    StatsCollector statsCollector(ServerConf serverConf, NotificationManager notificationManager) {
        if (serverConf.isUseMemoryStorage()) {
            return memoryStatsCollector(notificationManager);
        }
        String dbPath = serverConf.getStatsDbPath();
        if (dbPath == null || dbPath.isBlank()) {
            dbPath = Paths.get(TempDir.getTempDir(), DB_DEFAULT_NAME).toString();
        }
        DbStatsCollector dbStatsCollector = new DbStatsCollector(dbPath);
        notificationManager.querySubscribe(dbStatsCollector);
        return dbStatsCollector;
    }

}
