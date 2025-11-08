package com.allanvital.dnsao.dns.processor.engine.pojo;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.engine.unit.upstream.QueryOrchestrator;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamUnitConf {

    private final List<UpstreamResolver> resolvers;
    private final int multiplier;
    private final DNSSecMode dnsSecMode;
    private final boolean serveExpired;
    private final int timeout;
    private final QueryOrchestrator queryOrchestrator;

    public UpstreamUnitConf(List<UpstreamResolver> resolvers, int multiplier, DNSSecMode dnsSecMode, boolean serveExpired, int timeout, QueryOrchestrator queryOrchestrator) {
        this.resolvers = resolvers;
        this.multiplier = multiplier;
        this.dnsSecMode = dnsSecMode;
        this.serveExpired = serveExpired;
        this.timeout = timeout;
        this.queryOrchestrator = queryOrchestrator;
    }

    public List<UpstreamResolver> getResolvers() {
        return resolvers;
    }

    public int getMultiplier() {
        return multiplier;
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
}
