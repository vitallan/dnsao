package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.upstream.QueryOrchestrator;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;
import static org.xbill.DNS.Rcode.REFUSED;
import static org.xbill.DNS.Rcode.SERVFAIL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamUnit implements EngineUnit {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    private final boolean serveExpired;
    private final ExecutorServiceFactory executorServiceFactory;
    private final QueryOrchestrator queryOrchestrator;
    private final ResolverProvider resolverProvider;

    public UpstreamUnit(ExecutorServiceFactory executorServiceFactory, UpstreamUnitConf upstreamUnitConf) {
        this.serveExpired = upstreamUnitConf.isServeExpired();
        this.queryOrchestrator = upstreamUnitConf.getQueryOrchestrator();
        this.executorServiceFactory = executorServiceFactory;
        this.resolverProvider = upstreamUnitConf.getResolverProvider();
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        List<UpstreamResolver> upstreamsToBeUsed = resolverProvider.getResolversToUse();
        String threadName = Thread.currentThread().getName();
        ExecutorService executor = executorServiceFactory.buildExecutor(threadName + "-res", upstreamsToBeUsed.size());
        try {
            DnsQueryResult queryResult = queryOrchestrator.query(executor, dnsQueryRequest, upstreamsToBeUsed);
            if (queryResult == null) {
                return null;
            } else if (serveExpired && (isOfType(REFUSED, queryResult) || isOfType(SERVFAIL, queryResult))) {
                return null;
            }
            queryResult.cleanADHeader();
            return new DnsQueryResponse(dnsQueryRequest, queryResult);
        } catch (InterruptedException e) {
            log.warn("failed to resolve upstream: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        } finally {
            executor.shutdown();
        }
        return null;
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return UPSTREAM;
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
