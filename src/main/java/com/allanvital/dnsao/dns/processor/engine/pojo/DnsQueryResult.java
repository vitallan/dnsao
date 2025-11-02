package com.allanvital.dnsao.dns.processor.engine.pojo;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
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

    //since we are not internally validating dnssec, we must respond with no AD header
    public void cleanADHeader() {
        if (this.message == null) {
            return;
        }
        Header header = this.message.getHeader();
        header.unsetFlag(Flags.AD);
    }

}