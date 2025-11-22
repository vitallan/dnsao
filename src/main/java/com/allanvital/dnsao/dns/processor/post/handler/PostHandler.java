package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryEvent;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface PostHandler {

    void handle(DnsQueryRequest request, DnsQueryResponse response);

    default QueryEvent buildQueryEvent(DnsQueryRequest request, DnsQueryResponse response) {
        long elapsedNanos = response.getFinishTime() - request.getStart();
        long elapsedMillis = elapsedNanos / 1_000_000;
        return new QueryEvent(
                request.getClientAddress().getHostAddress(),
                response.getResponse(),
                response.getResponseSource(),
                response.getQueryResolvedBy(),
                elapsedMillis
        );
    }

}
