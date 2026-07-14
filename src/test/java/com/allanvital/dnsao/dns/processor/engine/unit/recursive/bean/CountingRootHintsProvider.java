package com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CountingRootHintsProvider implements RootHintsProvider {

    private final List<AuthorityEndpoint> rootHints;
    private int calls;

    public CountingRootHintsProvider(List<AuthorityEndpoint> rootHints) {
        this.rootHints = rootHints;
    }

    @Override
    public List<AuthorityEndpoint> getRootHints() {
        calls++;
        return rootHints;
    }

    public int getCalls() {
        return calls;
    }
}
