package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.recursive.RecursiveSession;
import com.allanvital.dnsao.dns.recursive.RecursiveSessionFactory;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.Message;

public class RecursiveUnit implements EngineUnit {

    private final RecursiveSessionFactory sessionFactory;

    public RecursiveUnit(RecursiveSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        RecursiveSession session = sessionFactory.createSession(dnsQueryRequest.getRequest());
        Message response = session.resolve();
        if (response == null) {
            return null;
        }
        return new DnsQueryResponse(dnsQueryRequest, response);
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return QueryResolvedBy.RECURSION;
    }

}
