package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class WinnerUpstreamHandler implements PostHandler{

    private final ResolverProvider resolverProvider;

    public WinnerUpstreamHandler(ResolverProvider resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        UpstreamResolver resolver = response.getResolver();
        if (resolver == null) {
            return;
        }
        resolverProvider.notifyLastWinner(resolver);
    }

}
