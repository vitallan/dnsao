package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.infra.log.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServerRacer {

    private static final int MAX_CONCURRENT_QUERIES = 2;

    private final ExecutorService executor;
    private final int timeoutSeconds;
    private final StepResolverFactory resolverFactory;
    private final DnssecDowngradeHandler dnssecHandler;
    private final RecursiveStatsCollector recursiveStatsCollector;

    public ServerRacer(ExecutorService executor, int timeoutSeconds, StepResolverFactory resolverFactory, DnssecDowngradeHandler dnssecHandler) {
        this(executor, timeoutSeconds, resolverFactory, dnssecHandler, new NoOpRecursiveStatsCollector());
    }

    public ServerRacer(ExecutorService executor, int timeoutSeconds, StepResolverFactory resolverFactory, DnssecDowngradeHandler dnssecHandler, RecursiveStatsCollector recursiveStatsCollector) {
        this.executor = executor;
        this.timeoutSeconds = timeoutSeconds;
        this.resolverFactory = resolverFactory;
        this.dnssecHandler = dnssecHandler;
        this.recursiveStatsCollector = recursiveStatsCollector;
    }

    public Map.Entry<NameServerAddress, StepResponse> race(List<NameServerAddress> servers, StepRequest request) {
        if (servers.isEmpty()) {
            return null;
        }
        if (servers.size() == 1) {
            return querySingleServer(servers.get(0), request);
        }

        recursiveStatsCollector.increment(RecursiveMetric.RACE_STARTED);
        recursiveStatsCollector.add(RecursiveMetric.RACE_CANDIDATE_SUM, servers.size());

        CompletionService<Map.Entry<NameServerAddress, StepResponse>> completionService = new ExecutorCompletionService<>(executor);
        List<QueryTask> tasks = new ArrayList<>();
        int submittedCount = 0;
        int runningCount = 0;
        int initialQueryCount = Math.min(servers.size(), MAX_CONCURRENT_QUERIES);
        recursiveStatsCollector.add(RecursiveMetric.RACE_FANOUT_SUM, initialQueryCount);
        Log.DNS.trace("recursive race started qtype={} qname={} candidates={} fanout={}", request.qtype(), request.qname(), servers.size(), initialQueryCount);
        for (int i = 0; i < initialQueryCount; i++) {
            QueryTask task = submitTask(servers.get(submittedCount), request, completionService);
            tasks.add(task);
            submittedCount++;
            runningCount++;
        }

        long deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        try {
            while (runningCount > 0) {
                long remainingNs = deadlineNs - System.nanoTime();
                if (remainingNs <= 0) {
                    break;
                }

                Future<Map.Entry<NameServerAddress, StepResponse>> completedFuture = completionService.poll(remainingNs, TimeUnit.NANOSECONDS);
                if (completedFuture == null) {
                    break;
                }
                runningCount--;

                Map.Entry<NameServerAddress, StepResponse> result = getFutureResult(completedFuture);
                if (submittedCount < servers.size()) {
                    QueryTask task = submitTask(servers.get(submittedCount), request, completionService);
                    tasks.add(task);
                    submittedCount++;
                    runningCount++;
                }
                if (result != null) {
                    int losersCancelled = countLosersToCancel(tasks, completedFuture);
                    recursiveStatsCollector.increment(RecursiveMetric.RACE_WON);
                    recursiveStatsCollector.add(RecursiveMetric.RACE_LOSERS_CANCELLED, losersCancelled);
                    Log.DNS.trace("recursive race winner qtype={} qname={} server={}:{} losersCancelled={}", request.qtype(), request.qname(), result.getKey().ip(), result.getKey().port(), losersCancelled);
                    cancelAndClose(tasks);
                    return result;
                }
            }
            recursiveStatsCollector.increment(RecursiveMetric.RACE_TIMEOUT);
            Log.DNS.trace("recursive race timeout qtype={} qname={} candidates={}", request.qtype(), request.qname(), servers.size());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            cancelAndClose(tasks);
        }
    }

    private Map.Entry<NameServerAddress, StepResponse> querySingleServer(NameServerAddress server, StepRequest request) {
        StepResolver resolver = resolverFactory.create(server.ip(), server.port());
        try {
            StepResponse response = dnssecHandler.queryWithPossibleDowngrade(resolver, request);
            if (response == null) {
                return null;
            }
            return new AbstractMap.SimpleEntry<>(server, response);
        } finally {
            resolver.close();
        }
    }

    private QueryTask submitTask(NameServerAddress server,
                                 StepRequest request,
                                 CompletionService<Map.Entry<NameServerAddress, StepResponse>> completionService) {
        StepResolver resolver = resolverFactory.create(server.ip(), server.port());
        Future<Map.Entry<NameServerAddress, StepResponse>> future = completionService.submit(() -> querySingleServer(server, request, resolver));
        return new QueryTask(resolver, future);
    }

    private Map.Entry<NameServerAddress, StepResponse> querySingleServer(NameServerAddress server,
                                                                         StepRequest request,
                                                                         StepResolver resolver) {
        try {
            StepResponse response = dnssecHandler.queryWithPossibleDowngrade(resolver, request);
            if (response == null) {
                return null;
            }
            return new AbstractMap.SimpleEntry<>(server, response);
        } finally {
            resolver.close();
        }
    }

    private Map.Entry<NameServerAddress, StepResponse> getFutureResult(Future<Map.Entry<NameServerAddress, StepResponse>> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void cancelAndClose(List<QueryTask> tasks) {
        for (QueryTask task : tasks) {
            task.resolver().close();
            task.future().cancel(true);
        }
    }

    private int countLosersToCancel(List<QueryTask> tasks, Future<Map.Entry<NameServerAddress, StepResponse>> winnerFuture) {
        int losers = 0;
        for (QueryTask task : tasks) {
            if (task.future() == winnerFuture) {
                continue;
            }
            if (!task.future().isDone()) {
                losers++;
            }
        }
        return losers;
    }

    private record QueryTask(StepResolver resolver, Future<Map.Entry<NameServerAddress, StepResponse>> future) {
    }

}
