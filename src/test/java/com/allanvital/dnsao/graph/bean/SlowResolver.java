package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.io.IOException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SlowResolver implements UpstreamResolver {

    private final long sleepTime;

    public SlowResolver() {
        this.sleepTime = 200;
    }

    public SlowResolver(long sleepTime) {
        this.sleepTime = sleepTime;
    }

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
        return "slow";
    }

    @Override
    public Message send(Message query) throws IOException {
        try {
            Thread.sleep(sleepTime);
            return MessageHelper.buildAResponse(query, "10.10.10.10", 300);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
