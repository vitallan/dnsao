package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface ResolverProvider {

    List<UpstreamResolver> getAllResolvers();

}
