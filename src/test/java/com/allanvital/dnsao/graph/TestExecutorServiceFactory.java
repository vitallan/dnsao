package com.allanvital.dnsao.graph;
import com.allanvital.dnsao.infra.log.Log;


import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestExecutorServiceFactory extends ExecutorServiceFactory {

    private final List<ExecutorService> executorServices = new LinkedList<>();

    @Override
    public ExecutorService buildExecutor(String poolName, int size) {
        ExecutorService service = super.buildExecutor(poolName, size);
        executorServices.add(service);
        return service;
    }

    @Override
    public ScheduledExecutorService buildScheduledExecutor(String poolName) {
        ScheduledExecutorService service = super.buildScheduledExecutor(poolName);
        executorServices.add(service);
        return service;
    }

    public void stopAndRemoveAllExecutors() throws InterruptedException {
        for (ExecutorService executorService : executorServices) {
            while (!executorService.isTerminated() || !executorService.isShutdown()) {
                Log.INFRA.trace("stopping {}", executorService);
                executorService.shutdownNow();
                executorService.awaitTermination(3, TimeUnit.SECONDS);
                Log.INFRA.trace("stopped {}", executorService);
            }
        }
        executorServices.clear();
    }

}
