package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.DnsSecPolicyException;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.RIGID;
import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamUnit implements EngineUnit {

    private static final Logger log = LoggerFactory.getLogger(DNS);
    private static final int MAX_RETRIES = 5;

    private final AtomicInteger index = new AtomicInteger(0);
    private final List<UpstreamResolver> resolvers;
    private final int multiplier;
    private final DNSSecMode dnsSecMode;
    private final boolean serveExpired;
    private final int timeoutInSecs;
    private final ExecutorServiceFactory executorServiceFactory;

    public UpstreamUnit(ExecutorServiceFactory executorServiceFactory, UpstreamUnitConf upstreamUnitConf) {
        this.resolvers = upstreamUnitConf.getResolvers();
        this.multiplier = upstreamUnitConf.getMultiplier();
        this.dnsSecMode = upstreamUnitConf.getDnsSecMode();
        this.serveExpired = upstreamUnitConf.isServeExpired();
        this.timeoutInSecs = upstreamUnitConf.getTimeout();
        this.executorServiceFactory = executorServiceFactory;
    }

    @Override
    public DnsQueryResponse process(DnsQueryRequest dnsQueryRequest) {
        List<UpstreamResolver> upstreamsToBeUsed = resolversToUse();
        try {
            DnsQueryResult queryResult = query(dnsQueryRequest.getRequest(), upstreamsToBeUsed, dnsSecMode, dnsQueryRequest.isLocalQuery(), MAX_RETRIES);
            if (queryResult == null) {
                return null;
            } else if (serveExpired && (isOfType(Rcode.REFUSED, queryResult) || isOfType(Rcode.SERVFAIL, queryResult))) {
                return null;
            }
            queryResult.cleanADHeader();
            return new DnsQueryResponse(dnsQueryRequest, queryResult);
        } catch (TimeoutException | InterruptedException e) {
            log.warn("failed to resolve upstream: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private List<UpstreamResolver> resolversToUse() {
        List<UpstreamResolver> paramResolvers = new LinkedList<>();
        int maxResolvers = Math.min(multiplier, resolvers.size());
        for (int i = 0; i < maxResolvers; i++) {
            int position = Math.floorMod(index.getAndIncrement(), resolvers.size());
            paramResolvers.add(resolvers.get(position));
        }
        return paramResolvers;
    }

    private DnsQueryResult query(Message query, List<UpstreamResolver> resolvers, DNSSecMode dnsSecMode, boolean isInternalQuery, int maxRetry)
            throws InterruptedException, TimeoutException {

        TimeoutException timeoutException = null;
        for (int i = 0; i < maxRetry; i++) {
            try {
                DnsQueryResult result = innerQuery(query, resolvers, dnsSecMode, isInternalQuery);
                if (result != null) {
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (TimeoutException e) {
                timeoutException = e;
            }
        }
        if (timeoutException != null) {
            throw timeoutException;
        }
        return null;
    }

    private DnsQueryResult innerQuery(Message query, List<UpstreamResolver> resolvers, DNSSecMode dnsSecMode, boolean isInternalQuery)
            throws InterruptedException, TimeoutException {

        String threadName = Thread.currentThread().getName();
        ExecutorService executor = executorServiceFactory.buildExecutor(threadName + "-res", resolvers.size());
        try {
            List<Callable<DnsQueryResult>> tasks = resolvers.stream()
                    .<Callable<DnsQueryResult>>map(resolver -> () -> {
                        Message response = resolver.send(query);
                        if (response == null) {
                            throw new IOException("Null response");
                        }
                        if (shouldRejectByDnssecPolicy(response, dnsSecMode)) {
                            throw new DnsSecPolicyException("non accepted answer based on dnssec policy");
                        }
                        return new DnsQueryResult(response, resolver);
                    })
                    .toList();

            return getResult(executor, tasks, isInternalQuery);
        } catch (ExecutionException e) {
            String question = query.getQuestion().toString();
            log.warn("query {} failed. Cause: {}", question, e.getMessage());
            return null;
        }
        finally {
            executor.shutdownNow();
        }
    }

    private DnsQueryResult getResult(ExecutorService executor, List<Callable<DnsQueryResult>> tasks, boolean isInternalQuery) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isInternalQuery) {
            return executor.invokeAny(tasks, timeoutInSecs, TimeUnit.SECONDS);
        }
        List<Future<DnsQueryResult>> futures = executor.invokeAll(tasks, timeoutInSecs, TimeUnit.SECONDS);

        long bestTtl = Long.MIN_VALUE;
        DnsQueryResult bestResult = null;
        for (Future<DnsQueryResult> future : futures) {
            if (future.isDone() && !future.isCancelled()) {
                DnsQueryResult dnsQueryResult = future.get();
                if (dnsQueryResult == null) {
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

    public boolean shouldRejectByDnssecPolicy(Message response, DNSSecMode dnsSecMode) {
        final boolean ad = response.getHeader().getFlag(Flags.AD);
        if (RIGID.equals(dnsSecMode)) {
            return !ad;
        }
        return false;
    }

    public boolean isOfType(int typeHeader, DnsQueryResult result) {
        Message message = result.message();
        if (message == null) {
            return false;
        }
        Header header = message.getHeader();
        return header.getRcode() == typeHeader;
    }

}
