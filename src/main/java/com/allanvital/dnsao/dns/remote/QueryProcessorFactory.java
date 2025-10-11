package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessorFactory {

    private final AtomicInteger index = new AtomicInteger(0);
    private Set<String> blockedDomains;
    private final CacheManager cacheManager;
    private final int multiplier;
    private final Map<String, String> localMappings;

    private final List<NamedResolver> resolvers;

    public QueryProcessorFactory(List<NamedResolver> resolvers, CacheManager cacheManager, Set<String> blockList, Map<String, String> localMappings, int multiplier) {
        this.resolvers = resolvers;
        this.cacheManager = cacheManager;
        this.blockedDomains = blockList;
        this.localMappings = localMappings;
        this.multiplier = multiplier;
    }

    public QueryProcessor buildQueryProcessor() {
        List<NamedResolver> paramResolvers = new LinkedList<>();
        int maxResolvers = multiplier;
        if (multiplier > resolvers.size()) {
            maxResolvers = resolvers.size();
        }
        for (int i = 0; i < maxResolvers; i++) {
            int position = Math.abs(index.getAndIncrement() % resolvers.size());
            paramResolvers.add(resolvers.get(position));
        }
        return new QueryProcessor(paramResolvers, cacheManager, blockedDomains, localMappings);
    }

}