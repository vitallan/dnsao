package com.allanvital.dnsao.dns.processor.engine.pojo;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.engine.unit.upstream.QueryOrchestrator;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamUnitConf {

    private final DNSSecMode dnsSecMode;
    private final boolean serveExpired;
    private final int timeout;
    private final QueryOrchestrator queryOrchestrator;
    private final ResolverProvider resolverProvider;

    public UpstreamUnitConf(DNSSecMode dnsSecMode,
                            boolean serveExpired,
                            int timeout,
                            QueryOrchestrator queryOrchestrator,
                            ResolverProvider resolverProvider) {

        this.dnsSecMode = dnsSecMode;
        this.serveExpired = serveExpired;
        this.timeout = timeout;
        this.queryOrchestrator = queryOrchestrator;
        this.resolverProvider = resolverProvider;
    }

    public DNSSecMode getDnsSecMode() {
        return dnsSecMode;
    }

    public boolean isServeExpired() {
        return serveExpired;
    }

    public int getTimeout() {
        return timeout;
    }

    public QueryOrchestrator getQueryOrchestrator() {
        return queryOrchestrator;
    }

    public ResolverProvider getResolverProvider() {
        return resolverProvider;
    }

}
