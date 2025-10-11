package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import com.allanvital.dnsao.conf.inner.ServerConf;
import com.allanvital.dnsao.conf.inner.Upstream;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Conf {

    private ServerConf server = new ServerConf();
    private ResolverConf resolver = new ResolverConf();
    private CacheConf cache = new CacheConf();

    public ServerConf getServer() {
        return server;
    }

    public void setServer(ServerConf server) {
        this.server = server;
    }

    public List<Upstream> getUpstreams() {
        return resolver.getUpstreams();
    }

    public void setUpstreams(List<Upstream> upstreams) {
        this.resolver.setUpstreams(upstreams);
    }

    public CacheConf getCache() {
        return cache;
    }

    public void setCache(CacheConf cache) {
        this.cache = cache;
    }

    public void setResolver(ResolverConf resolver) {
        this.resolver = resolver;
    }
    public ResolverConf getResolver() {
        return this.resolver;
    }

}