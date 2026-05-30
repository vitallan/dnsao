package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
            long deadline = System.currentTimeMillis() + 2000;
            boolean found = false;
            while (System.currentTimeMillis() < deadline && !found) {
                Set<Thread> threads = Thread.getAllStackTraces().keySet();
                for (Thread t : threads) {
                    if (t.getName() != null && t.getName().startsWith("upstream-")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Thread.sleep(10);
                }
            }
            assertTrue(found, "Expected at least one upstream-* thread after prestart");
        }
    }

    @Test
    public void callerRunsWhenPoolAndQueueAreFull() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 1)) {
            CountDownLatch block = new CountDownLatch(1);
            AtomicReference<String> callerThreadName = new AtomicReference<>();

            ex.executor().submit(() -> {
                await(block);
            });

            waitUntilActive(ex, 1, 2000);

            ex.executor().submit(() -> await(block));
            waitUntilQueued(ex, 1, 2000);

            String testThread = Thread.currentThread().getName();
            ex.executor().submit(() -> callerThreadName.set(Thread.currentThread().getName()));

            assertEquals(testThread, callerThreadName.get(), "Expected task to run on caller thread due to backpressure");
            block.countDown();
        }
    }

    @Test
    public void closeShutsDownExecutor() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 1);
        ex.executor().submit(() -> {}).get(2, TimeUnit.SECONDS);
        ex.close();

        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && !ex.executor().isShutdown()) {
            Thread.sleep(10);
        }
        assertTrue(ex.executor().isShutdown());
    }

    @Test
    public void queuedTaskRunsAfterWorkerFrees() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 10)) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            AtomicBoolean ran = new AtomicBoolean(false);
            AtomicReference<String> ranThread = new AtomicReference<>();

            ex.executor().submit(() -> {
                started.countDown();
                await(block);
            });

            assertTrue(started.await(2, TimeUnit.SECONDS), "Expected worker task to start");
            waitUntilActive(ex, 1, 2000);

            ex.executor().submit(() -> {
                ranThread.set(Thread.currentThread().getName());
                ran.set(true);
            });

            Thread.sleep(50);
            assertFalse(ran.get(), "Expected task to be queued while worker is blocked");
            assertTrue(ex.getQueuedTaskCount() >= 1, "Expected at least one queued task");

            block.countDown();
            waitUntilTrue(ran, 2000);
            assertNotNull(ranThread.get());
            assertTrue(ranThread.get().startsWith("upstream-"), "Expected queued task to run on upstream thread");
        }
    }

    @Test
    public void queuedTasksDrainAfterWorkerFrees() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor ex = new UpstreamThreadPoolExecutor(factory, 1, 10)) {
            CountDownLatch block = new CountDownLatch(1);
            CountDownLatch started = new CountDownLatch(1);
            List<String> events = new CopyOnWriteArrayList<>();

            ex.executor().submit(() -> {
                started.countDown();
                await(block);
            });

            assertTrue(started.await(2, TimeUnit.SECONDS), "Expected worker task to start");
            waitUntilActive(ex, 1, 2000);

            ex.executor().submit(() -> events.add("b"));
            ex.executor().submit(() -> events.add("c"));

            Thread.sleep(50);
            assertTrue(events.isEmpty(), "Expected queued tasks not to run while worker is blocked");
            assertTrue(ex.getQueuedTaskCount() >= 2, "Expected queuedTaskCount >= 2 while worker blocked");

            block.countDown();
            waitUntilSize(events, 2, 2000);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waitUntilActive(UpstreamThreadPoolExecutor ex, int expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (ex.getActiveCount() >= expected) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Expected activeCount >= " + expected + " but was " + ex.getActiveCount());
    }

    private static void waitUntilQueued(UpstreamThreadPoolExecutor ex, int expected, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (ex.getQueuedTaskCount() >= expected) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Expected queuedTaskCount >= " + expected + " but was " + ex.getQueuedTaskCount());
    }

    private static void waitUntilTrue(AtomicBoolean flag, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (flag.get()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Expected flag to become true");
    }

    private static void waitUntilSize(List<?> list, int expectedSize, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (list.size() >= expectedSize) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Expected list size >= " + expectedSize + " but was " + list.size());
    }

}
