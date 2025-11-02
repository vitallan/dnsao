package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.EventType;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.infra.notification.QueryEvent;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NotificationPostHandler implements PostHandler {

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        NotificationManager notifier = NotificationManager.getInstance();
        notifier.notify(EventType.QUERY_RESOLVED);

        if (!request.isLocalQuery()) {
            long elapsedNanos = response.getFinishTime() - request.getStart();
            long elapsedMillis = elapsedNanos / 1_000_000;
            QueryEvent queryEvent = new QueryEvent(
                    request.getClientAddress().getHostAddress(),
                    response.getResponse(),
                    response.getResponseSource(),
                    response.getQueryResolvedBy(),
                    elapsedMillis
            );
            notifier.notifyQuery(queryEvent);
        }

    }

}
