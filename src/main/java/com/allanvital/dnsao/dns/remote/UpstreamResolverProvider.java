package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamResolverProvider implements ResolverProvider {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicReference<UpstreamResolver> lastWinner = new AtomicReference<>();
    private final int multiplier;
    private final UpstreamResolverBuilder resolverBuilder;

    public UpstreamResolverProvider(UpstreamResolverBuilder resolverBuilder, int multiplier) {
        this.multiplier = multiplier;
        this.resolverBuilder = resolverBuilder;
    }

    @Override
    public List<UpstreamResolver> getAllResolvers() {
        return resolverBuilder.getAllResolvers();
    }

    @Override
    public List<UpstreamResolver> getResolversToUse() {
        List<UpstreamResolver> resolvers = getAllResolvers();
        List<UpstreamResolver> resolversToUse = new LinkedList<>();
        int i = 0;
        int maxResolvers = Math.min(multiplier, resolvers.size());
        UpstreamResolver lastWinner = getLastWinner();
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

    private UpstreamResolver getLastWinner() {
        List<UpstreamResolver> resolvers = getAllResolvers();
        if (lastWinner.get() == null || multiplier >= resolvers.size()) {
            return null;
        }
        return lastWinner.get();
    }

    @Override
    public void notifyLastWinner(UpstreamResolver resolver) {
        lastWinner.set(resolver);
    }

}