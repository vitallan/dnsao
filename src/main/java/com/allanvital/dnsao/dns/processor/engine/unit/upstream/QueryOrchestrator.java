package com.allanvital.dnsao.dns.processor.engine.unit.upstream;

import com.allanvital.dnsao.cache.CacheEntryFactory;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryOrchestrator {

    private final int timeoutInSecs;
    private final DNSSecMode dnsSecMode;
    private final int maxRetries;
    private final CacheEntryFactory cacheEntryFactory;

    public QueryOrchestrator(int timeoutInSecs, DNSSecMode dnsSecMode, int maxRetries, CacheEntryFactory cacheEntryFactory) {
        this.timeoutInSecs = timeoutInSecs;
        this.dnsSecMode = dnsSecMode;
        this.maxRetries = maxRetries;
        this.cacheEntryFactory = cacheEntryFactory;
    }

    public DnsQueryResult query(ExecutorService executorService, DnsQueryRequest request, List<UpstreamResolver> resolvers) throws InterruptedException {
        Message query = request.getRequest();
        boolean isLocalQuery = request.isLocalQuery();
        TaskExecutorAndDecider taskExecutorAndDecider = new TaskExecutorAndDecider(executorService, timeoutInSecs, isLocalQuery, cacheEntryFactory);
        TaskProvider taskProvider = new TaskProvider(resolvers, dnsSecMode);
        QueryExecutor executor = new QueryExecutor(taskProvider, taskExecutorAndDecider, maxRetries);
        return executor.executeWithRetry(query);
    }

}
