package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface PostHandler {

    void handle(DnsQueryRequest request, DnsQueryResponse response);

}
