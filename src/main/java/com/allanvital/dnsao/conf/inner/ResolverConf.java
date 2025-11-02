package com.allanvital.dnsao.conf.inner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ResolverConf {

    private List<Upstream> upstreams = buildDefault();
    private int multiplier = 1;
    private int tlsPoolSize = 3;
    private List<String> blocklists = new LinkedList<>();
    private List<String> allowLists = new LinkedList<>();
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

    public List<String> getBlocklists() {
        return blocklists;
    }

    public void setBlocklists(List<String> blocklists) {
        this.blocklists = blocklists;
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

    public List<String> getAllowLists() {
        return allowLists;
    }

    public void setAllowLists(List<String> allowLists) {
        this.allowLists = allowLists;
    }

}