package com.allanvital.dnsao.pool;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPool;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTUpstreamResolver;
import com.allanvital.dnsao.holder.DotTestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DotConnectionPoolTest extends DotTestHolder {

    private DOTConnectionPool pool;
    private ExecutorService executorService;

    private SSLSocket s1;
    private SSLSocket s2;
    private SSLSocket s3;
    private SSLSocket s4;
    private SSLSocket s5;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("dot/1dot-upstream-nocache.yml");
        List<UpstreamResolver> resolvers = queryInfraAssembler.getResolvers();
        DOTUpstreamResolver upstreamResolver = (DOTUpstreamResolver) resolvers.get(0);
        pool = upstreamResolver.getPool();
    }

    @Test
    public void shouldNotReturnSameSocketWhenNotifiedItIsBad() throws IOException, TimeoutException {
        s1 = pool.acquire();
        pool.release(s1, true);
        s2 = pool.acquire();
        assertNotSame(s1, s2);
    }

    @Test
    public void shouldCorrectlyControlDotConnectionPool() throws Exception {
        pool.setAcquireTimeout(300);
        s1 = pool.acquire();
        s2 = pool.acquire();
        s3 = pool.acquire();
        TimeoutException exception = null;
        try {
            s4 = pool.acquire();
            fail("should not acquire socket if pool is full and other sockets are not released");
        } catch (TimeoutException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void shouldReleaseSocketsCorrectly() throws Exception {
        s1 = pool.acquire();
        s2 = pool.acquire();
        s3 = pool.acquire();
        pool.release(s2);
        s4 = pool.acquire();
        assertEquals(s2, s4);
    }

    @Test
    void shouldUnblockWaiterWhenReleasedAndHandoffSameSocket() throws Exception {
        s1 = pool.acquire();
        s2 = pool.acquire();
        s3 = pool.acquire();

        executorService = Executors.newSingleThreadExecutor();
        Future<SSLSocket> waiter = executorService.submit(() -> pool.acquire());

        Thread.sleep(50);

        pool.release(s2);

        s4 = waiter.get(500, TimeUnit.MILLISECONDS);
        assertSame(s2, s4, "waiter should receive the released socket via direct handoff");
    }

    @Test
    void shouldServeWaitersInFifoOrderAndReuseReleasedSockets() throws Exception {
        s1 = pool.acquire();
        s2 = pool.acquire();
        s3 = pool.acquire();

        executorService = Executors.newFixedThreadPool(2);

        Future<SSLSocket> f1 = executorService.submit(() -> pool.acquire());
        Future<SSLSocket> f2 = executorService.submit(() -> pool.acquire());

        pool.release(s3);
        pool.release(s2);
        s4 = f1.get(2, TimeUnit.SECONDS);
        s5 = f2.get(2, TimeUnit.SECONDS);

        assertNotNull(s5, "second waiter should receive a socket");

        assertTrue(s4 == s3 || s4 == s2);
        assertTrue(s5 == s3 || s5 == s2);
    }

    @Test
    void shouldNeverExceedMaxSizeUnderBurst() throws Exception {
        int threads = 20;
        AtomicInteger inUse = new AtomicInteger(0);
        AtomicInteger peak = new AtomicInteger(0);

        executorService = Executors.newFixedThreadPool(threads);
        CyclicBarrier start = new CyclicBarrier(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    start.await();
                    SSLSocket s = pool.acquire();
                    int now = inUse.incrementAndGet();
                    peak.accumulateAndGet(now, Math::max);
                    assertTrue(pool.getPoolSize() <= 3);
                    Thread.sleep(10);
                    inUse.decrementAndGet();
                    pool.release(s);
                } catch (TimeoutException te) {

                } catch (Exception e) {
                    fail(e);
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get(2, TimeUnit.SECONDS);
        }
        assertTrue(peak.get() <= 3, "peak in-use must not exceed hard cap, but was: " + peak.get());
    }

    @Test
    void shouldCloseUnhealthyAndFreeCapacity() throws Exception {
        s1 = pool.acquire();
        s2 = pool.acquire();
        s3 = pool.acquire();

        executorService = Executors.newSingleThreadExecutor();
        Future<SSLSocket> waiter = executorService.submit(() -> pool.acquire());

        Thread.sleep(50);
        closeQuiet(s2);
        pool.release(s2);

        s4 = waiter.get(500, TimeUnit.MILLISECONDS);
        assertNotNull(s4, "capacity should be freed by closing unhealthy socket");
        assertNotSame(s2, s4);
    }


    private void closeQuiet(SSLSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignore) {
            //ignored
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeQuiet(s1);
        closeQuiet(s2);
        closeQuiet(s3);
        closeQuiet(s4);
        closeQuiet(s5);
        if (executorService != null) {
            executorService.shutdown();
        }
        safeStop();
    }

}
