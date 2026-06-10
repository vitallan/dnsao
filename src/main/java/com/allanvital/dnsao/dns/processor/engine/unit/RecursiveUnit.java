package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.recursive.RecursiveSession;
import com.allanvital.dnsao.dns.recursive.RecursiveSessionFactory;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;

public class RecursiveUnit implements EngineUnit {

    private final RecursiveSessionFactory recursiveSessionFactory;

    public RecursiveUnit(RecursiveSessionFactory recursiveSessionFactory) {
        this.recursiveSessionFactory = recursiveSessionFactory;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        RecursiveSession session = recursiveSessionFactory.createSession(dnsQueryRequest);
        Message response = session.resolve();
        if (response == null) {
            return null;
        }
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setFlag(Flags.RA);
        if (hasRdFlag(dnsQueryRequest)) {
            response.getHeader().setFlag(Flags.RD);
        }
        return new DnsQueryResponse(dnsQueryRequest, response);
    }

    private boolean hasRdFlag(DnsQueryRequest dnsQueryRequest) {
        if (dnsQueryRequest == null || dnsQueryRequest.getRequest() == null || dnsQueryRequest.getRequest().getHeader() == null) {
            return false;
        }
        return dnsQueryRequest.getRequest().getHeader().getFlag(Flags.RD);
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return QueryResolvedBy.RECURSION;
    }

}
