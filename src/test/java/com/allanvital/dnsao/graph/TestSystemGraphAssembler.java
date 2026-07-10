package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.keep.KeepProvider;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.MutableState;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestSystemGraphAssembler extends SystemGraphAssembler {

    private CacheManager cacheManager;
    private QueryProcessorFactory queryProcessorFactory;
    private TestQueryInfraAssembler queryInfraAssembler;
    private MutableState mutableState;

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
    protected MutableState createMutableState(boolean blockingEnabled) {
        this.mutableState = super.createMutableState(blockingEnabled);
        return this.mutableState;
    }

    @Override
    QueryProcessorFactory queryProcessorFactory(QueryProcessorDependencies queryProcessorDependencies) {
        this.queryProcessorFactory =  super.queryProcessorFactory(queryProcessorDependencies);
        return this.queryProcessorFactory;
    }

    @Override
    QueryInfraAssembler queryInfraAssembler() {
        this.queryInfraAssembler = new TestQueryInfraAssembler(overrideRegistry);
        return this.queryInfraAssembler;
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

    public MutableState getMutableState() {
        return mutableState;
    }

}
