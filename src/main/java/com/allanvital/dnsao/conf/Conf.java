package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Conf {

    private ServerConf server = new ServerConf();
    private ResolverConf resolver = new ResolverConf();
    private CacheConf cache = new CacheConf();
    private MiscConf misc = new MiscConf();
    private ListsConf lists = new ListsConf();
    private Map<String, GroupInnerConf> groups = new HashMap<>();

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

    public ListsConf getLists() {
        return lists;
    }

    public void setLists(ListsConf lists) {
        this.lists = lists;
    }

    public Map<String, GroupInnerConf> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, GroupInnerConf> groups) {
        this.groups = groups;
    }

    public void sanitizeGroups() {
        Set<String> validAllows = lists.getValidAllowNames();
        Set<String> validBlocks = lists.getValidBlockNames();
        Set<Map.Entry<String, GroupInnerConf>> entries = groups.entrySet();
        for (Map.Entry<String, GroupInnerConf> entry : entries) {
            if (MAIN.equalsIgnoreCase(entry.getKey())) {
                groups.remove(entry.getKey());
                continue;
            }
            GroupInnerConf group = entry.getValue();
            group.getAllows().removeIf(allow -> !validAllows.contains(allow));
            group.getBlocks().removeIf(block -> !validBlocks.contains(block));
        }
        GroupInnerConf mainInnerConf = new GroupInnerConf();
        mainInnerConf.setAllows(validAllows);
        mainInnerConf.setBlocks(validBlocks);
        groups.put(MAIN, mainInnerConf);
    }

}