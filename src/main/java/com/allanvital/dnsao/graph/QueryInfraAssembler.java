package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.block.BlockListProvider;
import com.allanvital.dnsao.dns.block.DownloadFileHandler;
import com.allanvital.dnsao.dns.block.FileHandler;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.QueryEngine;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.post.PostHandlerFacade;
import com.allanvital.dnsao.dns.processor.post.PostHandlerProvider;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerFacade;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerProvider;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.utils.DownloadUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryInfraAssembler {

    private final OverrideRegistry overrideRegistry;

    public QueryInfraAssembler(OverrideRegistry overrideRegistry) {
        this.overrideRegistry = overrideRegistry;
    }

    public QueryProcessorDependencies assemble(ResolverConf resolverConf, MiscConf miscConf, CacheManager cacheManager, ExecutorServiceFactory executorServiceFactory) throws ConfException {
        DOTConnectionPoolManager poolManager = dotConnectionPoolManager(resolverConf.getTlsPoolSize());
        ResolverProvider resolverProvider = resolverProvider(poolManager, resolverConf.getUpstreams());

        UpstreamUnitConf upstreamUnitConf = upstreamUnitConf(resolverProvider, resolverConf, miscConf);
        Map<String, String> locaMappings = localMappings(resolverConf.getLocalMappings());
        FileHandler fileHandler = fileHandler();
        BlockListProvider blockListProvider = blockListProvider(executorServiceFactory.buildScheduledExecutor("block"), resolverConf.getAllowLists(), resolverConf.getBlocklists(), fileHandler, miscConf.isRefreshLists());

        PreHandlerProvider preHandlerProvider = preHandlerProvider(miscConf.getDnsSecMode());
        EngineUnitProvider engineUnitProvider = engineUnitProvider(executorServiceFactory, blockListProvider, locaMappings, cacheManager, upstreamUnitConf);
        PostHandlerProvider postHandlerProvider = postHandlerProvider(cacheManager);

        PreHandlerFacade preHandlerFacade = preHandlerFacade(preHandlerProvider);
        QueryEngine queryEngine = queryEngine(engineUnitProvider);
        PostHandlerFacade postHandlerFacade = postHandlerFacade(postHandlerProvider, executorServiceFactory);

        return new QueryProcessorDependencies(preHandlerFacade, queryEngine, postHandlerFacade);
    }

    private DOTConnectionPoolManager dotConnectionPoolManager(int tlsPoolSize) {
        return new DOTConnectionPoolManager(tlsPoolSize);
    }

    private ResolverProvider resolverProvider(DOTConnectionPoolManager dotConnectionPoolManager, List<Upstream> upstreams) {
        return overrideRegistry.getRegisteredModule(ResolverProvider.class)
                .orElse(new UpstreamResolverProvider(dotConnectionPoolManager, upstreams));
    }

    private UpstreamUnitConf upstreamUnitConf(ResolverProvider resolverProvider, ResolverConf resolverConf, MiscConf miscConf) {
        return new UpstreamUnitConf(resolverProvider.getAllResolvers(), resolverConf.getMultiplier(), miscConf.getDnsSecMode(), miscConf.isServeExpired(), miscConf.getTimeout());
    }

    private Map<String, String> localMappings(List<LocalMapping> localMappings) {
        Map<String, String> mappings = new HashMap<>();
        for (LocalMapping localMapping : localMappings) {
            mappings.put(localMapping.getNormalizedDomain(), localMapping.getIp());
        }
        return mappings;
    }

    private FileHandler fileHandler() throws ConfException {
        Optional<FileHandler> registeredModule = overrideRegistry.getRegisteredModule(FileHandler.class);
        if (registeredModule.isPresent()) {
            return registeredModule.get();
        }
        Path workDir = null;
        try {
            workDir = DownloadUtils.getAppDir();
            return new DownloadFileHandler(workDir);
        } catch (IOException e) {
            throw new ConfException(e.getMessage());
        }
    }

    public BlockListProvider blockListProvider(ScheduledExecutorService scheduledExecutorService, List<String> blockList, List<String> allowList, FileHandler fileHandler, boolean refreshLists) {
        return new BlockListProvider(scheduledExecutorService, blockList, allowList, fileHandler, refreshLists);
    }

    private PreHandlerFacade preHandlerFacade(PreHandlerProvider preHandlerProvider) {
        return new PreHandlerFacade(preHandlerProvider);
    }

    private PreHandlerProvider preHandlerProvider(DNSSecMode dnsSecMode) {
        return new PreHandlerProvider(dnsSecMode);
    }

    private QueryEngine queryEngine(EngineUnitProvider engineUnitProvider) {
        return new QueryEngine(engineUnitProvider);
    }

    private EngineUnitProvider engineUnitProvider(ExecutorServiceFactory executorServiceFactory,
                                                  BlockListProvider blockListProvider,
                                                  Map<String, String> localMappings,
                                                  CacheManager cacheManager,
                                                  UpstreamUnitConf upstreamUnitConf) {

        return new EngineUnitProvider(executorServiceFactory, blockListProvider, localMappings, cacheManager, upstreamUnitConf);
    }

    private PostHandlerFacade postHandlerFacade(PostHandlerProvider provider, ExecutorServiceFactory executorServiceFactory) {
        return new PostHandlerFacade(provider, executorServiceFactory);
    }

    private PostHandlerProvider postHandlerProvider(CacheManager cacheManager) {
        return new PostHandlerProvider(cacheManager);
    }

}
