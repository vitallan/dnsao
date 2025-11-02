package com.allanvital.dnsao.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestExecutorServiceFactory extends ExecutorServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(INFRA);
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
            while (!executorService.isTerminated()) {
                log.info("stopping " + executorService);
                executorService.shutdownNow();
                executorService.awaitTermination(3, TimeUnit.SECONDS);
                log.info("stopped " + executorService);
            }
        }
        executorServices.clear();
    }

}
