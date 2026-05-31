package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class UpstreamThreadPoolExecutorTest {

    @Test
    public void defaults_whenNullConfig() {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, null, null)) {
            assertEquals(UpstreamThreadPoolExecutor.DEFAULT_POOL_SIZE, ex.getPoolSize());
            assertEquals(UpstreamThreadPoolExecutor.DEFAULT_QUEUE_SIZE, ex.getQueueSize());
        }
    }

    @Test
    public void invalidPoolSize_defaultsTo64() {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 0, 10)) {
            assertEquals(UpstreamThreadPoolExecutor.DEFAULT_POOL_SIZE, ex.getPoolSize());
        }
    }

    @Test
    public void invalidQueueSize_defaultsTo640() {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 2, 0)) {
            assertEquals(UpstreamThreadPoolExecutor.DEFAULT_QUEUE_SIZE, ex.getQueueSize());
        }
    }

    @Test
    public void prestartsCoreThreads_andUsesUpstreamPrefix() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 2, 2)) {
            CountDownLatch ran = new CountDownLatch(1);

            ex.executor().execute(ran::countDown);

            assertTrue(ran.await(2, TimeUnit.SECONDS), "Expected a task to run on the upstream executor");
        }
    }

    @Test
    public void callerRunsWhenPoolAndQueueAreFull() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 1)) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch workerStarted = new CountDownLatch(1);
            AtomicReference<String> callerThreadName = new AtomicReference<>();
            CountDownLatch callerRan = new CountDownLatch(1);

            ExecutorService svc = ex.executor();
            ThreadPoolExecutor tp = (ThreadPoolExecutor) svc;

            try {
                svc.execute(() -> {
                    workerStarted.countDown();
                    await(block);
                });
                assertTrue(workerStarted.await(2, TimeUnit.SECONDS), "Expected worker task to start");

                svc.execute(() -> await(block));
                waitUntilQueueFull(tp, 2000);

                assertEquals(1, tp.getQueue().size());
                assertEquals(0, tp.getQueue().remainingCapacity());

                String testThread = Thread.currentThread().getName();
                svc.execute(() -> {
                    callerThreadName.set(Thread.currentThread().getName());
                    callerRan.countDown();
                });

                assertTrue(callerRan.await(1, TimeUnit.SECONDS), "Expected caller-runs task to run");
                assertEquals(testThread, callerThreadName.get(), "Expected task to run on caller thread due to backpressure");
            } finally {
                block.countDown();
            }
        }
    }

    @Test
    public void closeShutsDownExecutor() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 1);
        ex.executor().submit(() -> {}).get(2, TimeUnit.SECONDS);
        ex.close();
        assertTrue(ex.executor().isShutdown());
    }

    @Test
    public void queuedTaskRunsAfterWorkerFrees() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 10)) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            AtomicInteger ranCount = new AtomicInteger(0);
            CountDownLatch ranLatch = new CountDownLatch(1);

            ExecutorService svc = ex.executor();
            ThreadPoolExecutor tp = (ThreadPoolExecutor) svc;

            try {
                svc.execute(() -> {
                    started.countDown();
                    await(block);
                });

                assertTrue(started.await(2, TimeUnit.SECONDS), "Expected worker task to start");
                assertEquals(0, tp.getQueue().size());

                svc.execute(() -> {
                    ranCount.incrementAndGet();
                    ranLatch.countDown();
                });

                assertEquals(1, tp.getQueue().size());

                assertFalse(ranLatch.await(150, TimeUnit.MILLISECONDS), "Expected task to be queued while worker is blocked");
                assertEquals(0, ranCount.get());

                block.countDown();

                assertTrue(ranLatch.await(2, TimeUnit.SECONDS), "Expected queued task to run after worker frees");
                assertEquals(1, ranCount.get());
            } finally {
                block.countDown();
            }
        }
    }

    @Test
    public void queuedTasksDrainAfterWorkerFrees() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 10)) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AtomicInteger doneCount = new AtomicInteger(0);

            ExecutorService svc = ex.executor();
            ThreadPoolExecutor tp = (ThreadPoolExecutor) svc;

            try {
                svc.execute(() -> {
                    started.countDown();
                    await(block);
                });

                assertTrue(started.await(2, TimeUnit.SECONDS), "Expected worker task to start");
                assertEquals(0, tp.getQueue().size());

                svc.execute(() -> {
                    doneCount.incrementAndGet();
                    done.countDown();
                });
                svc.execute(() -> {
                    doneCount.incrementAndGet();
                    done.countDown();
                });

                assertEquals(2, tp.getQueue().size());
                assertFalse(done.await(150, TimeUnit.MILLISECONDS), "Expected queued tasks not to run while worker is blocked");
                assertEquals(0, doneCount.get());

                block.countDown();
                assertTrue(done.await(2, TimeUnit.SECONDS), "Expected queued tasks to drain after worker frees");
                assertEquals(2, doneCount.get());
            } finally {
                block.countDown();
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitUntilQueueFull(ThreadPoolExecutor tp, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (tp.getQueue().remainingCapacity() == 0) {
                return;
            }
            Thread.sleep(5);
        }
        fail("Expected upstream queue to be full but remainingCapacity was " + tp.getQueue().remainingCapacity() + ", size=" + tp.getQueue().size());
    }

}
