package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import com.allanvital.dnsao.dns.local.LocalResolver;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessorFactory {

    private final AtomicInteger index = new AtomicInteger(0);
    private final CacheManager cacheManager;
    private final LocalResolver localResolver;
    private final int multiplier;
    private final DNSSecMode dnsSecMode;

    private final List<UpstreamResolver> resolvers;

    public QueryProcessorFactory(List<UpstreamResolver> resolvers, CacheManager cacheManager, LocalResolver localResolver, ResolverConf conf) {
        this(resolvers, cacheManager, localResolver, conf.getMultiplier(), conf.getDnsSecMode());
    }

    public QueryProcessorFactory(List<UpstreamResolver> resolvers, CacheManager cacheManager, LocalResolver localResolver, int multiplier, DNSSecMode dnsSecMode) {
        this.resolvers = resolvers;
        this.cacheManager = cacheManager;
        this.localResolver = localResolver;
        this.multiplier = multiplier;
        this.dnsSecMode = dnsSecMode;
    }

    public QueryProcessor buildQueryProcessor() {
        return new QueryProcessor(getResolvers(), cacheManager, localResolver, dnsSecMode);
    }

    public QueryProcessor buildCacheLessQueryProcessor() {
        return new QueryProcessor(getResolvers(), null, localResolver, dnsSecMode);
    }

    private List<UpstreamResolver> getResolvers() {
        List<UpstreamResolver> paramResolvers = new LinkedList<>();
        int maxResolvers = Math.min(multiplier, resolvers.size());
        for (int i = 0; i < maxResolvers; i++) {
            int position = Math.abs(index.getAndIncrement() % resolvers.size());
            paramResolvers.add(resolvers.get(position));
        }
        return paramResolvers;
    }

}