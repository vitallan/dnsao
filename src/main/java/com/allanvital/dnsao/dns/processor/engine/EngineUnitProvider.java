package com.allanvital.dnsao.dns.processor.engine;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.inner.ResolverMode;
import com.allanvital.dnsao.dns.block.BlockDecider;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.*;
import com.allanvital.dnsao.dns.remote.UpstreamThreadPoolExecutor;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class EngineUnitProvider {

    private final List<EngineUnit> engineUnits = new LinkedList<>();

    public EngineUnitProvider(ExecutorServiceFactory executorServiceFactory,
                              UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                              BlockDecider blockDecider,
                              Map<String, String> localMappings,
                              CacheManager cacheManager,
                              UpstreamUnitConf upstreamUnitConf,
                              boolean blockingEnabled,
                              RecursiveUnit recursiveUnit,
                              ResolverMode resolverMode) {

        engineUnits.add(new BlockUnit(blockDecider, blockingEnabled));
        engineUnits.add(new LocalMappingUnit(localMappings));
        engineUnits.add(new CacheUnit(cacheManager));

        if (resolverMode == ResolverMode.RECURSIVE) {
            engineUnits.add(recursiveUnit);
        } else {
            engineUnits.add(new UpstreamUnit(upstreamThreadPoolExecutor, upstreamUnitConf));
        }

        engineUnits.add(new StaleUnit(cacheManager));
        engineUnits.add(new ServFailUnit());
    }

    public List<EngineUnit> getOrderedEngineUnits() {
        return engineUnits;
    }

}
