package com.allanvital.dnsao.dns.remote.pojo;

import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsQueryResult {

    private final Message message;
    private final NamedResolver resolver;

    public DnsQueryResult(Message message, NamedResolver resolver) {
        this.message = message;
        this.resolver = resolver;
    }

    public Message message() { return message; }
    public NamedResolver resolver() { return resolver; }

}