package com.allanvital.dnsao.graph;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ExecutorServiceFactory {

    private final List<ExecutorService> executorServices = Collections.synchronizedList(new LinkedList<>());

    public ThreadFactory buildThreadFactory(String poolName) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                int andIncrement = threadNumber.getAndIncrement();
                t.setName(String.format("%s-%02d", poolName, andIncrement));
                return t;
            }
        };
    }

    public ExecutorService buildExecutor(String poolName, int size) {
        ExecutorService executorService = Executors.newFixedThreadPool(size, buildThreadFactory(poolName));
        executorServices.add(executorService);
        return executorService;
    }

    public ExecutorService buildCachedExecutor(String poolName) {
        ExecutorService executorService = Executors.newCachedThreadPool(buildThreadFactory(poolName));
        executorServices.add(executorService);
        return executorService;
    }

    public ScheduledExecutorService buildScheduledExecutor(String poolName) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(buildThreadFactory(poolName));
        executorServices.add(executorService);
        return executorService;
    }

    public void closeAllExecutors() {
        synchronized (executorServices) {
            for (ExecutorService executorService : executorServices) {
                executorService.shutdownNow();
                try {
                    executorService.awaitTermination(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            executorServices.clear();
        }
    }

}
