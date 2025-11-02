package com.allanvital.dnsao.dns.processor.engine;

import com.allanvital.dnsao.dns.block.BlockListProvider;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.*;
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
                              BlockListProvider blockListProvider,
                              Map<String, String> localMappings,
                              CacheManager cacheManager,
                              UpstreamUnitConf upstreamUnitConf) {

        engineUnits.add(new BlockListUnit(blockListProvider));
        engineUnits.add(new LocalMappingUnit(localMappings));
        engineUnits.add(new CacheUnit(cacheManager));
        engineUnits.add(new UpstreamUnit(executorServiceFactory, upstreamUnitConf));
        engineUnits.add(new StaleUnit(cacheManager));
        engineUnits.add(new ServFailUnit());
    }

    public List<EngineUnit> getOrderedEngineUnits() {
        return engineUnits;
    }

}
