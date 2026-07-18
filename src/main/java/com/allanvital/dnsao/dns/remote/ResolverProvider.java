package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface ResolverProvider {

    List<UpstreamResolver> getAllResolvers();
    List<UpstreamResolver> getResolversToUse();
    default List<UpstreamResolver> getResolversToUse(UpstreamRoutingPolicy routingPolicy) {
        return getResolversToUse();
    }

    default void notifyLastWinner(UpstreamResolver resolver) {

    }

    default void notifyLastWinner(UpstreamResolver resolver, UpstreamRoutingPolicy routingPolicy) {
        notifyLastWinner(resolver);
    }

}
