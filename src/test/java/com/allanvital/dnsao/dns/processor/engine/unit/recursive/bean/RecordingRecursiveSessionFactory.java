package com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RecursiveSession;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RecursiveSessionFactory;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecordingRecursiveSessionFactory extends RecursiveSessionFactory {

    private final RecursiveSession recursiveSession;
    private DnsQueryRequest seenRequest;
    private List<AuthorityEndpoint> seenRootHints;

    public RecordingRecursiveSessionFactory(RecursiveSession recursiveSession) {
        super(null, null);
        this.recursiveSession = recursiveSession;
    }

    @Override
    public RecursiveSession buildRecursiveSession(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        this.seenRequest = dnsQueryRequest;
        this.seenRootHints = rootHints;
        return recursiveSession;
    }

    public DnsQueryRequest getSeenRequest() {
        return seenRequest;
    }

    public List<AuthorityEndpoint> getSeenRootHints() {
        return seenRootHints;
    }
}
