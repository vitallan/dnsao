package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.UPSTREAM_PRIORITIZED;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class WinnerUpstreamHandler implements PostHandler{

    private final ResolverProvider resolverProvider;

    public WinnerUpstreamHandler(ResolverProvider resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        UpstreamResolver resolver = response.getResolver();
        if (resolver == null || request.isLocalQuery()) {
            return;
        }
        resolverProvider.notifyLastWinner(resolver);
        telemetryNotify(UPSTREAM_PRIORITIZED);
    }

}
