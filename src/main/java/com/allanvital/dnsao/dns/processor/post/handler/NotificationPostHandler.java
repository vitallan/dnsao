package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.NotificationManager;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NotificationPostHandler implements PostHandler {

    private final NotificationManager notifier;

    public NotificationPostHandler(NotificationManager notificationManager) {
        this.notifier = notificationManager;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        if (!request.isLocalQuery()) {
            notifier.notifyQuery(buildQueryEvent(request, response));
        }

    }

}
