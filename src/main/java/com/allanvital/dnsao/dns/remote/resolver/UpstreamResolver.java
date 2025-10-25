package com.allanvital.dnsao.dns.remote.resolver;

import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface UpstreamResolver {

    String getIp();
    int getPort();
    Message send(Message query) throws IOException;

    default String name() {
        return getIp() + ":" + getPort();
    }

}