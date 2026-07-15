package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionContext {

    private final DnsQueryRequest dnsQueryRequest;
    private final List<AuthorityEndpoint> rootHints;

    public RecursiveSessionContext(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.rootHints = List.copyOf(rootHints);
    }

    public DnsQueryRequest getDnsQueryRequest() {
        return dnsQueryRequest;
    }

    public List<AuthorityEndpoint> getRootHints() {
        return rootHints;
    }
}
