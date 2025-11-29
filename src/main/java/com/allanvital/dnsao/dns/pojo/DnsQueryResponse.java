package com.allanvital.dnsao.dns.pojo;

import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsQueryResponse {

    private DnsQueryRequest dnsQueryRequest;
    private Message response;
    private QueryResolvedBy queryResolvedBy;
    private String responseSource;
    private long finishTime;
    private UpstreamResolver resolver;

    public DnsQueryResponse(DnsQueryRequest dnsQueryRequest, Message response) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.response = response;
    }

    public DnsQueryResponse(DnsQueryRequest dnsQueryRequest, DnsQueryResult upstreamResult) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.response = upstreamResult.message();
        this.responseSource = upstreamResult.resolver().name();
        this.resolver = upstreamResult.resolver();
    }

    public DnsQueryRequest getDnsQueryRequest() {
        return dnsQueryRequest;
    }

    public void setDnsQueryRequest(DnsQueryRequest dnsQueryRequest) {
        this.dnsQueryRequest = dnsQueryRequest;
    }

    public String getResponseSource() {
        return responseSource;
    }

    public void setResponseSource(String responseSource) {
        this.responseSource = responseSource;
    }

    public Message getResponse() {
        return response;
    }

    public void setResponse(Message response) {
        this.response = response;
    }

    public QueryResolvedBy getQueryResolvedBy() {
        return queryResolvedBy;
    }

    public void setQueryResolvedBy(QueryResolvedBy queryResolvedBy) {
        this.queryResolvedBy = queryResolvedBy;
    }

    public void markFinishTime() {
        this.finishTime = System.nanoTime();
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void resetToOriginalId(DnsQueryRequest dnsQueryRequest) {
        this.response.getHeader().setID(dnsQueryRequest.getOriginalRequestId());
    }

    public UpstreamResolver getResolver() {
        return resolver;
    }
}
