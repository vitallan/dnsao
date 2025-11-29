package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestResolverProvider implements ResolverProvider {

    private final List<UpstreamResolver> resolvers;

    public TestResolverProvider(List<UpstreamResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public List<UpstreamResolver> getAllResolvers() {
        return resolvers;
    }

    @Override
    public List<UpstreamResolver> getResolversToUse() {
        return resolvers;
    }

}
