package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestQueryInfraAssembler extends QueryInfraAssembler {

    private ResolverProvider resolverProvider;
    private UpstreamResolverBuilder resolverBuilder;

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

    public ResolverProvider getResolverProvider() {
        return resolverProvider;
    }

    public List<UpstreamResolver> getResolvers() {
        return resolverProvider.getAllResolvers();
    }

    public UpstreamResolverBuilder getResolverBuilder() {
        return resolverBuilder;
    }

}
