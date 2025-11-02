package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.ExpiredConf;
import com.allanvital.dnsao.conf.inner.MiscConf;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.exc.ConfException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestSystemGraphAssembler extends SystemGraphAssembler {

    private CacheManager cacheManager;
    private QueryProcessorFactory queryProcessorFactory;
    private QueryProcessorDependencies dependencies;

    public TestSystemGraphAssembler() {
        super();
    }

    @Override
    CacheManager cacheManager(CacheConf cacheConf, FixedTimeRewarmScheduler fixedTimeRewarmScheduler, ExpiredConf expiredConf) {
        this.cacheManager = super.cacheManager(cacheConf, fixedTimeRewarmScheduler, expiredConf);
        return this.cacheManager;
    }

    @Override
    QueryProcessorFactory queryProcessorFactory(QueryProcessorDependencies queryProcessorDependencies) {
        this.queryProcessorFactory =  super.queryProcessorFactory(queryProcessorDependencies);
        return this.queryProcessorFactory;
    }

    @Override
    public QueryProcessorDependencies queryProcessorDependencies(ExecutorServiceFactory executorServiceFactory, ResolverConf resolverConf, MiscConf miscConf, CacheManager cacheManager) throws ConfException {
        QueryProcessorDependencies dependencies = super.queryProcessorDependencies(executorServiceFactory, resolverConf, miscConf, cacheManager);
        this.dependencies = dependencies;
        return dependencies;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public QueryProcessorFactory getQueryProcessorFactory() {
        return queryProcessorFactory;
    }

    public QueryProcessorDependencies getQueryProcessorDependencies() {
        return dependencies;
    }

}
