package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StaticRootHintsProvider implements RootHintsProvider {

    private final List<AuthorityEndpoint> rootHints;

    public StaticRootHintsProvider() {
        this.rootHints = List.of(buildDefaultRootHint());
    }

    public StaticRootHintsProvider(List<AuthorityEndpoint> rootHints) {
        this.rootHints = List.copyOf(rootHints);
    }

    @Override
    public List<AuthorityEndpoint> getRootHints() {
        return List.copyOf(rootHints);
    }

    private static AuthorityEndpoint buildDefaultRootHint() {
        try {
            return new AuthorityEndpoint("a.root-servers.net.", InetAddress.getByName("198.41.0.4"), 53);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("failed to create default root hint", e);
        }
    }
}
