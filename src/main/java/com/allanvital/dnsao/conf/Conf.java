package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.*;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Conf {

    private ServerConf server = new ServerConf();
    private ResolverConf resolver = new ResolverConf();
    private CacheConf cache = new CacheConf();
    private MiscConf misc = new MiscConf();

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
        if (cache == null) {
            cache = new CacheConf();
        }
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

    public MiscConf getMisc() {
        return misc;
    }

    public void setMisc(MiscConf misc) {
        this.misc = misc;
    }
}