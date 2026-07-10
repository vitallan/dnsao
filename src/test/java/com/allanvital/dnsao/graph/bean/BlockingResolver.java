package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingResolver implements UpstreamResolver {

    private final String ip;
    private final long ttl;
    public final CountDownLatch started = new CountDownLatch(1);
    public final CountDownLatch proceed = new CountDownLatch(1);

    public BlockingResolver(String ip, long ttl) {
        this.ip = ip;
        this.ttl = ttl;
    }

    @Override
    public String getIp() {
        return "blocking";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public Message send(Message query) throws IOException {
        started.countDown();
        try {
            if (!proceed.await(2, TimeUnit.SECONDS)) {
                throw new IOException("timed out waiting for test to release resolver");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        return MessageHelper.buildAResponse(query, ip, ttl);
    }
}
