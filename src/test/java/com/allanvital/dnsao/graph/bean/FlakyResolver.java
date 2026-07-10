package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class FlakyResolver implements UpstreamResolver {

    private final String ip;
    private final long ttl;
    private final AtomicInteger attempts = new AtomicInteger();
    public final CountDownLatch firstAttempt = new CountDownLatch(1);
    public final CountDownLatch secondAttempt = new CountDownLatch(1);

    public FlakyResolver(String ip, long ttl) {
        this.ip = ip;
        this.ttl = ttl;
    }

    @Override
    public String getIp() {
        return "flaky";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public Message send(Message query) throws IOException {
        int attempt = attempts.incrementAndGet();
        if (attempt == 1) {
            firstAttempt.countDown();
            throw new IOException("transient failure");
        }
        secondAttempt.countDown();
        return MessageHelper.buildAResponse(query, ip, ttl);
    }
}
