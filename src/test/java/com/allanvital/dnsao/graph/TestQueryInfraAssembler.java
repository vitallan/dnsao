package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.block.BlockDecider;
import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestQueryInfraAssembler extends QueryInfraAssembler {

    private ResolverProvider resolverProvider;
    private UpstreamResolverBuilder resolverBuilder;
    private EngineUnitProvider engineUnitProvider;

    public TestQueryInfraAssembler(OverrideRegistry overrideRegistry) {
        super(overrideRegistry);
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
                                          BlockDecider blockDecider,
                                          Map<String, String> localMappings,
                                          CacheManager cacheManager,
                                          UpstreamUnitConf upstreamUnitConf) {

        this.engineUnitProvider = super.engineUnitProvider(executorServiceFactory, blockDecider, localMappings, cacheManager, upstreamUnitConf);
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

}
