package com.allanvital.dnsao.dns.remote;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamResolverProvider implements ResolverProvider {


    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicReference<UpstreamResolver> lastWinner = new AtomicReference<>();
    private final Map<String, UpstreamResolver> lastWinnerByPolicy = new ConcurrentHashMap<>();
    private final int multiplier;
    private final UpstreamResolverBuilder resolverBuilder;
    private final Map<String, GroupInnerConf> groups;

    public UpstreamResolverProvider(UpstreamResolverBuilder resolverBuilder, int multiplier) {
        this(resolverBuilder, multiplier, Map.of());
    }

    public UpstreamResolverProvider(UpstreamResolverBuilder resolverBuilder, int multiplier, Map<String, GroupInnerConf> groups) {
        this.multiplier = multiplier;
        this.resolverBuilder = resolverBuilder;
        this.groups = groups;
    }

    @Override
    public List<UpstreamResolver> getAllResolvers() {
        return resolverBuilder.getAllResolvers();
    }

    @Override
    public List<UpstreamResolver> getResolversToUse() {
        List<UpstreamResolver> resolvers = getAllResolvers();
        return selectResolvers(resolvers, lastWinner.get());
    }

    @Override
    public List<UpstreamResolver> getResolversToUse(UpstreamRoutingPolicy routingPolicy) {
        List<UpstreamResolver> resolvers = getResolversForPolicy(routingPolicy);
        UpstreamResolver winner = routingPolicy == null ? lastWinner.get() : lastWinnerByPolicy.get(policyKey(routingPolicy));
        return selectResolvers(resolvers, winner);
    }

    @Override
    public List<UpstreamResolver> getSingleResolverToUse(UpstreamRoutingPolicy routingPolicy) {
        List<UpstreamResolver> resolvers = getResolversForPolicy(routingPolicy);
        UpstreamResolver winner = routingPolicy == null ? lastWinner.get() : lastWinnerByPolicy.get(policyKey(routingPolicy));
        UpstreamResolver selected = selectSingleResolver(resolvers, winner);
        if (selected == null) {
            return List.of();
        }
        return List.of(selected);
    }

    private List<UpstreamResolver> getResolversForPolicy(UpstreamRoutingPolicy routingPolicy) {
        List<UpstreamResolver> allResolvers = getAllResolvers();
        if (routingPolicy == null || groups == null || groups.isEmpty()) {
            return allResolvers;
        }
        GroupInnerConf group = groups.get(routingPolicy.group());
        if (group == null || group.getUpstreams().isEmpty()) {
            group = groups.get(MAIN);
        }
        if (group == null || group.getUpstreams().isEmpty()) {
            return allResolvers;
        }

        List<UpstreamResolver> selectedResolvers = new LinkedList<>();
        for (String upstreamName : group.getUpstreams()) {
            UpstreamResolver resolver = resolverBuilder.getNamedResolvers().get(upstreamName);
            if (resolver == null) {
                Log.INFRA.error("unknown upstream name {} on group {}; using full resolver pool", upstreamName, routingPolicy.group());
                return allResolvers;
            }
            selectedResolvers.add(resolver);
        }
        return selectedResolvers;
    }

    private List<UpstreamResolver> selectResolvers(List<UpstreamResolver> resolvers, UpstreamResolver winner) {
        List<UpstreamResolver> resolversToUse = new LinkedList<>();
        if (resolvers.isEmpty()) {
            return resolversToUse;
        }
        if (multiplier >= resolvers.size()) {
            if (winner != null && resolvers.contains(winner)) {
                resolversToUse.add(winner);
            }
            for (UpstreamResolver resolver : resolvers) {
                if (!resolver.equals(winner)) {
                    resolversToUse.add(resolver);
                }
            }
            return resolversToUse;
        }
        int i = 0;
        int maxResolvers = Math.min(multiplier, resolvers.size());
        UpstreamResolver lastWinner = getLastWinner(resolvers, winner);
        if (maxResolvers > 1 && lastWinner != null) {
            resolversToUse.add(lastWinner);
            i++;
        }
        while (i < maxResolvers) {
            int position = Math.floorMod(index.getAndIncrement(), resolvers.size());
            UpstreamResolver upstreamResolver = resolvers.get(position);
            if (upstreamResolver.equals(lastWinner)) {
                continue;
            }
            resolversToUse.add(upstreamResolver);
            i++;
        }
        return resolversToUse;
    }

    private UpstreamResolver getLastWinner(List<UpstreamResolver> resolvers, UpstreamResolver winner) {
        if (winner == null || !resolvers.contains(winner)) {
            return null;
        }
        return winner;
    }

    private UpstreamResolver selectSingleResolver(List<UpstreamResolver> resolvers, UpstreamResolver winner) {
        UpstreamResolver lastWinner = getLastWinner(resolvers, winner);
        if (lastWinner != null) {
            return lastWinner;
        }
        if (resolvers.isEmpty()) {
            return null;
        }
        int position = Math.floorMod(index.getAndIncrement(), resolvers.size());
        return resolvers.get(position);
    }

    @Override
    public void notifyLastWinner(UpstreamResolver resolver, UpstreamRoutingPolicy routingPolicy) {
        if (routingPolicy == null) {
            lastWinner.set(resolver);
            return;
        }
        lastWinnerByPolicy.put(policyKey(routingPolicy), resolver);
    }

    private String policyKey(UpstreamRoutingPolicy routingPolicy) {
        return routingPolicy.group();
    }

}
