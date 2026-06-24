package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.infra.notification.NotificationManager;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestSystemGraphAssembler extends SystemGraphAssembler {

    private CacheManager cacheManager;
    private QueryProcessorFactory queryProcessorFactory;
    private TestQueryInfraAssembler queryInfraAssembler;
    private NotificationManager notificationManager;
    private Integer recursiveMaxNsNameResolutions;
    private Long recursiveMaxSessionElapsedMs;

    public TestSystemGraphAssembler() {
        super();
    }

    @Override
    CacheManager cacheManager(CacheConf cacheConf,
                              FixedTimeRewarmScheduler fixedTimeRewarmScheduler,
                              ExpiredConf expiredConf,
                              KeepProvider keepProvider) {
        this.cacheManager = super.cacheManager(cacheConf, fixedTimeRewarmScheduler, expiredConf, keepProvider);
        return this.cacheManager;
    }

    @Override
    QueryProcessorFactory queryProcessorFactory(QueryProcessorDependencies queryProcessorDependencies) {
        this.queryProcessorFactory =  super.queryProcessorFactory(queryProcessorDependencies);
        return this.queryProcessorFactory;
    }

    @Override
    QueryInfraAssembler queryInfraAssembler() {
        this.queryInfraAssembler = new TestQueryInfraAssembler(overrideRegistry);
        if (recursiveMaxNsNameResolutions != null && recursiveMaxSessionElapsedMs != null) {
            this.queryInfraAssembler.setRecursiveBudgetOverrides(recursiveMaxNsNameResolutions, recursiveMaxSessionElapsedMs);
        }
        return this.queryInfraAssembler;
    }

    @Override
    NotificationManager notificationManager(boolean queryLogEnabled) {
        this.notificationManager = new NotificationManager(queryLogEnabled);
        return this.notificationManager;
    }

    public TestQueryInfraAssembler getQueryInfraAssembler() {
        return this.queryInfraAssembler;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public QueryProcessorFactory getQueryProcessorFactory() {
        return queryProcessorFactory;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public void setRecursiveBudgetOverrides(int maxNsNameResolutions, long maxSessionElapsedMs) {
        this.recursiveMaxNsNameResolutions = maxNsNameResolutions;
        this.recursiveMaxSessionElapsedMs = maxSessionElapsedMs;
        if (queryInfraAssembler != null) {
            queryInfraAssembler.setRecursiveBudgetOverrides(maxNsNameResolutions, maxSessionElapsedMs);
        }
    }

}
