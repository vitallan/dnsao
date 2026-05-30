package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

public class UpstreamThreadPoolExecutor implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    public static final int DEFAULT_POOL_SIZE = 64;
    public static final int DEFAULT_QUEUE_SIZE = DEFAULT_POOL_SIZE * 10;

    private final ThreadPoolExecutor executor;
    private final int poolSize;
    private final int queueSize;

    public UpstreamThreadPoolExecutor(ExecutorServiceFactory executorServiceFactory, Integer poolSize, Integer queueSize) {
        int ps = (poolSize == null) ? DEFAULT_POOL_SIZE : poolSize;
        int qs = (queueSize == null) ? DEFAULT_QUEUE_SIZE : queueSize;

        if (ps <= 0) {
            log.warn("resolver.upstreamThreadPoolSize is invalid ({}). Defaulting to {}", ps, DEFAULT_POOL_SIZE);
            ps = DEFAULT_POOL_SIZE;
        }
        if (qs <= 0) {
            log.warn("resolver.upstreamQueueSize is invalid ({}). Defaulting to {}", qs, DEFAULT_QUEUE_SIZE);
            qs = DEFAULT_QUEUE_SIZE;
        }

        this.poolSize = ps;
        this.queueSize = qs;
        this.executor = new ThreadPoolExecutor(
                ps,
                ps,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(qs),
                executorServiceFactory.buildThreadFactory("upstream"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.executor.prestartAllCoreThreads();
    }

    public ExecutorService executor() {
        return executor;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getQueuedTaskCount() {
        return executor.getQueue().size();
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
