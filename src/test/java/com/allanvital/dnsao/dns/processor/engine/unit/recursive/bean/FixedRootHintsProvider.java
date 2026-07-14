package com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FixedRootHintsProvider implements RootHintsProvider {

    private final List<AuthorityEndpoint> rootHints;

    public FixedRootHintsProvider(List<AuthorityEndpoint> rootHints) {
        this.rootHints = rootHints;
    }

    @Override
    public List<AuthorityEndpoint> getRootHints() {
        return rootHints;
    }
}
