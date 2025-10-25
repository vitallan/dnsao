package com.allanvital.dnsao.helper;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeResolverWithBarrier implements UpstreamResolver {

    private final String expectedIp = "100.100.100.100";

    private final CyclicBarrier barrier;
    private final boolean winner;
    private final AtomicInteger callCounter;

    public FakeResolverWithBarrier(CyclicBarrier barrier, boolean winner, AtomicInteger callCounter) {
        this.barrier = barrier;
        this.winner = winner;
        this.callCounter = callCounter;
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
    public Message send(Message query) throws IOException {
        callCounter.incrementAndGet();
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (BrokenBarrierException e) {
            throw new IOException("Barrier broken", e);
        }
        if (winner) {
            return MessageUtils.buildAResponse(query, expectedIp, 1000);
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new IOException("Loser resolver");
        }
    }
}
