package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RecursiveSession;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RecursiveSessionFactory;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RootHintsProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;

import java.util.List;

import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveUnit implements EngineUnit {

    private final RecursiveSessionFactory recursiveSessionFactory;
    private final RootHintsProvider rootHintsProvider;

    public RecursiveUnit(RecursiveSessionFactory recursiveSessionFactory, RootHintsProvider rootHintsProvider) {
        this.recursiveSessionFactory = recursiveSessionFactory;
        this.rootHintsProvider = rootHintsProvider;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        List<AuthorityEndpoint> rootHints = rootHintsProvider.getRootHints();
        RecursiveSession recursiveSession = recursiveSessionFactory.buildRecursiveSession(dnsQueryRequest, rootHints);
        RecursiveResult recursiveResult = recursiveSession.resolve();
        if (recursiveResult == null || recursiveResult.getFinalMessage() == null) {
            return null;
        }
        return new DnsQueryResponse(dnsQueryRequest, recursiveResult.getFinalMessage());
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return SERVFAIL;
    }

}
