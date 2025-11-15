package com.allanvital.dnsao.conf.inner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ResolverConf {

    private List<Upstream> upstreams = buildDefault();
    private int multiplier = 1;
    private int tlsPoolSize = 3;
    private List<LocalMapping> localMappings = new LinkedList<>();

    public List<Upstream> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(List<Upstream> upstreams) {
        this.upstreams = upstreams;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public int getTlsPoolSize() {
        return tlsPoolSize;
    }

    public void setTlsPoolSize(int tlsPoolSize) {
        this.tlsPoolSize = tlsPoolSize;
    }

    public List<LocalMapping> getLocalMappings() {
        if (this.localMappings == null) {
            return new LinkedList<>();
        }
        return localMappings;
    }

    public void setLocalMappings(List<LocalMapping> localMappings) {
        this.localMappings = localMappings;
    }

    private static List<Upstream> buildDefault() {
        List<Upstream> upstreams = new ArrayList<>();
        Upstream upstream = new Upstream();
        upstream.setIp("9.9.9.9");
        upstream.setPort(53);
        upstream.setProtocol("udp");
        upstreams.add(upstream);
        return upstreams;
    }

}