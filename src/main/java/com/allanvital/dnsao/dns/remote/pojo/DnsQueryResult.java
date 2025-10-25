package com.allanvital.dnsao.dns.remote.pojo;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsQueryResult {

    private final Message message;
    private final UpstreamResolver resolver;

    public DnsQueryResult(Message message, UpstreamResolver resolver) {
        this.message = message;
        this.resolver = resolver;
    }

    public Message message() { return message; }
    public UpstreamResolver resolver() { return resolver; }

}