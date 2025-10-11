package com.allanvital.dnsao.dns.remote.resolver;

import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface NamedResolver {

    String getIp();
    int getPort();
    Message send(Message query) throws IOException;

}