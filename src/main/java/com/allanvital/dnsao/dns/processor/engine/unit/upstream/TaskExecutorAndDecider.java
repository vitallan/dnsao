package com.allanvital.dnsao.dns.processor.engine.unit.upstream;

import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.DnsUtils;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TaskExecutorAndDecider {

    private final ExecutorService executor;
    private final int timeoutInSecs;
    private final boolean isInternalQuery;

    public TaskExecutorAndDecider(ExecutorService executor, int timeoutInSecs, boolean isInternalQuery) {
        this.executor = executor;
        this.timeoutInSecs = timeoutInSecs;
        this.isInternalQuery = isInternalQuery;
    }

    public DnsQueryResult executeAndPickResult(List<Callable<DnsQueryResult>> tasks) throws InterruptedException, ExecutionException, TimeoutException {
        if (isInternalQuery) {
            return getResultToInternalClient(tasks);
        }
        return getResultToExternalClient(tasks);
    }

    private DnsQueryResult getResultToInternalClient(List<Callable<DnsQueryResult>> tasks) throws InterruptedException {
        List<Future<DnsQueryResult>> futures = executor.invokeAll(tasks, timeoutInSecs, TimeUnit.SECONDS);

        long bestTtl = Long.MIN_VALUE;
        DnsQueryResult bestResult = null;
        for (Future<DnsQueryResult> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                DnsQueryResult dnsQueryResult;
                try {
                    dnsQueryResult = future.get();
                    if (dnsQueryResult == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                } catch (ExecutionException | CancellationException e) {
                    continue;
                }
                Long ttl = DnsUtils.getTtlFromDirectResponse(dnsQueryResult.message());
                if (ttl > bestTtl) {
                    bestResult = dnsQueryResult;
                    bestTtl = ttl;
                }
            }
        }
        return bestResult;
    }

    private DnsQueryResult getResultToExternalClient(List<Callable<DnsQueryResult>> tasks) throws ExecutionException, InterruptedException, TimeoutException {
        return executor.invokeAny(tasks, timeoutInSecs, TimeUnit.SECONDS);
    }

}
