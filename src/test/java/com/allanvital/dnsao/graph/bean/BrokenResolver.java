package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BrokenResolver implements UpstreamResolver {

    @Override
    public String getIp() {
        return "";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public String name() {
        return "broken";
    }

    @Override
    public Message send(Message query) throws IOException {
        throw new IOException("This one is broken");
    }

}
