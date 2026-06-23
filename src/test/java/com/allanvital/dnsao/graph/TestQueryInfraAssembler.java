package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.MiscConf;
import com.allanvital.dnsao.conf.inner.ResolverMode;
import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.block.BlockDecider;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.recursive.RecursiveStatsCollector;
import com.allanvital.dnsao.dns.recursive.StepResolverFactory;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamThreadPoolExecutor;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.infra.notification.NotificationManager;

import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestQueryInfraAssembler extends QueryInfraAssembler {

    private ResolverProvider resolverProvider;
    private UpstreamResolverBuilder resolverBuilder;
    private EngineUnitProvider engineUnitProvider;
    private TestStepResolverFactory testStepResolverFactory;
    private RecursiveStatsCollector recursiveStatsCollector;

    public TestQueryInfraAssembler(OverrideRegistry overrideRegistry) {
        super(overrideRegistry);
    }

    @Override
    public QueryProcessorDependencies assemble(Conf conf,
                                               CacheManager cacheManager,
                                               ExecutorServiceFactory executorServiceFactory,
                                               UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                                               NotificationManager notificationManager) throws ConfException {
        return super.assemble(conf, cacheManager, executorServiceFactory, upstreamThreadPoolExecutor, notificationManager);
    }

    @Override
    StepResolverFactory stepResolverFactory(MiscConf miscConf, RecursiveStatsCollector recursiveStatsCollector) {
        testStepResolverFactory = new TestStepResolverFactory(miscConf.getTimeout(), recursiveStatsCollector);
        return testStepResolverFactory;
    }

    @Override
    RecursiveStatsCollector recursiveStatsCollector() {
        this.recursiveStatsCollector = super.recursiveStatsCollector();
        return this.recursiveStatsCollector;
    }

    @Override
    ResolverProvider resolverProvider(UpstreamResolverBuilder resolverBuilder, int multiplier) {
        this.resolverProvider = super.resolverProvider(resolverBuilder, multiplier);
        return resolverProvider;
    }

    @Override
    UpstreamResolverBuilder upstreamResolverBuilder(DOTConnectionPoolFactory connectionPoolFactory, List<Upstream> upstreams) {
        resolverBuilder = super.upstreamResolverBuilder(connectionPoolFactory, upstreams);
        return resolverBuilder;
    }

    @Override
    EngineUnitProvider engineUnitProvider(ExecutorServiceFactory executorServiceFactory,
                                          UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                                          BlockDecider blockDecider,
                                          Map<String, String> localMappings,
                                          CacheManager cacheManager,
                                          UpstreamUnitConf upstreamUnitConf,
                                          boolean blockingEnabled,
                                          RecursiveUnit recursiveUnit,
                                          ResolverMode resolverMode) {

        this.engineUnitProvider = super.engineUnitProvider(executorServiceFactory, upstreamThreadPoolExecutor, blockDecider, localMappings, cacheManager, upstreamUnitConf, blockingEnabled, recursiveUnit, resolverMode);
        return this.engineUnitProvider;
    }

    public ResolverProvider getResolverProvider() {
        return resolverProvider;
    }

    public List<UpstreamResolver> getResolvers() {
        return resolverProvider.getAllResolvers();
    }

    public UpstreamResolverBuilder getResolverBuilder() {
        return resolverBuilder;
    }

    public EngineUnitProvider getEngineUnitProvider() {
        return engineUnitProvider;
    }

    public TestStepResolverFactory getTestStepResolverFactory() {
        return testStepResolverFactory;
    }

    public RecursiveStatsCollector getRecursiveStatsCollector() {
        return recursiveStatsCollector;
    }

}
