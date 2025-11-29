package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QuickResolver implements UpstreamResolver, Counter {

    private final AtomicInteger counter = new AtomicInteger(0);

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
        return "quick";
    }

    @Override
    public Message send(Message query) {
        counter.incrementAndGet();
        return MessageHelper.buildAResponse(query, "10.10.10.10", 2);
    }

    @Override
    public int getCount() {
        return counter.get();
    }

}
