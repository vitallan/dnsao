package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestQueryInfraAssembler extends QueryInfraAssembler {

    private ResolverProvider resolverProvider;

    public TestQueryInfraAssembler(OverrideRegistry overrideRegistry) {
        super(overrideRegistry);
    }

    @Override
    ResolverProvider resolverProvider(DOTConnectionPoolFactory dotConnectionPoolFactory, List<Upstream> upstreams) {
        resolverProvider = super.resolverProvider(dotConnectionPoolFactory, upstreams);
        return resolverProvider;
    }

    public ResolverProvider getResolverProvider() {
        return resolverProvider;
    }

    public List<UpstreamResolver> getResolvers() {
        return resolverProvider.getAllResolvers();
    }

}
