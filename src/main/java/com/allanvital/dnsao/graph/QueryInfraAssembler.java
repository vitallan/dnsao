package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import com.allanvital.dnsao.dns.block.*;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.QueryEngine;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.upstream.QueryOrchestrator;
import com.allanvital.dnsao.dns.processor.post.PostHandlerFacade;
import com.allanvital.dnsao.dns.processor.post.PostHandlerProvider;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerFacade;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerProvider;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.infra.notification.NotificationManager;

import javax.net.ssl.SSLSocketFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryInfraAssembler {

    protected final OverrideRegistry overrideRegistry;

    public QueryInfraAssembler(OverrideRegistry overrideRegistry) {
        this.overrideRegistry = overrideRegistry;
    }

    public QueryProcessorDependencies assemble(Conf conf, CacheManager cacheManager, ExecutorServiceFactory executorServiceFactory, NotificationManager notificationManager) throws ConfException {
        ResolverConf resolverConf = conf.getResolver();
        MiscConf miscConf = conf.getMisc();
        ListsConf listsConf = conf.getLists();

        DOTConnectionPoolFactory dotConnectionPoolFactory = dotConnectionPoolFactory((SSLSocketFactory) SSLSocketFactory.getDefault(), resolverConf.getTlsPoolSize());
        ResolverProvider resolverProvider = resolverProvider(dotConnectionPoolFactory, resolverConf.getUpstreams());
        QueryOrchestrator orchestrator = queryOrchestrator(miscConf);
        UpstreamUnitConf upstreamUnitConf = upstreamUnitConf(resolverProvider, resolverConf, miscConf, orchestrator);
        Map<String, String> locaMappings = localMappings(resolverConf.getLocalMappings());
        DomainListFileReader domainListFileReader = domainListFileReader();
        Refresher refresher = refresher(executorServiceFactory);
        FileListsProvider fileListsProvider = fileListsProvider(refresher, listsConf, domainListFileReader, miscConf.isRefreshLists());
        BlockDecider blockDecider = blockDecider(fileListsProvider, listsConf, conf.getGroups());

        PreHandlerProvider preHandlerProvider = preHandlerProvider(miscConf.getDnsSecMode());
        EngineUnitProvider engineUnitProvider = engineUnitProvider(executorServiceFactory, blockDecider, locaMappings, cacheManager, upstreamUnitConf);

        PostHandlerProvider postHandlerProvider = postHandlerProvider(cacheManager, notificationManager, conf.getListeners().getHttp());

        PreHandlerFacade preHandlerFacade = preHandlerFacade(preHandlerProvider);
        QueryEngine queryEngine = queryEngine(engineUnitProvider);
        PostHandlerFacade postHandlerFacade = postHandlerFacade(postHandlerProvider, executorServiceFactory);

        return new QueryProcessorDependencies(preHandlerFacade, queryEngine, postHandlerFacade);
    }

    private Refresher refresher(ExecutorServiceFactory executorServiceFactory) {
        return overrideRegistry.getRegisteredModule(Refresher.class)
                .orElse(new ListRefresher(executorServiceFactory.buildScheduledExecutor("refresh-lists")));
    }

    private DomainListFileReader domainListFileReader() {
        return overrideRegistry.getRegisteredModule(DomainListFileReader.class)
                .orElse(new DownloadListFileReader());
    }

    private FileListsProvider fileListsProvider(Refresher refresher, ListsConf listsConf, DomainListFileReader domainListFileReader, boolean refreshLists) {
        return overrideRegistry.getRegisteredModule(FileListsProvider.class)
                .orElse(new FileListsProvider(refresher, listsConf, domainListFileReader, refreshLists));
    }

    private BlockDecider blockDecider(FileListsProvider fileListsProvider, ListsConf listsConf, Map<String, GroupInnerConf> groups) {
        return overrideRegistry.getRegisteredModule(BlockDecider.class)
                .orElse(new BlockDecider(fileListsProvider, listsConf, groups));
    }

    ResolverProvider resolverProvider(DOTConnectionPoolFactory connectionPoolFactory, List<Upstream> upstreams) {
        return overrideRegistry.getRegisteredModule(ResolverProvider.class)
                .orElse(new UpstreamResolverProvider(connectionPoolFactory, upstreams));
    }

    DOTConnectionPoolFactory dotConnectionPoolFactory(SSLSocketFactory socketFactory, int maxPoolSize) {
        return new DOTConnectionPoolFactory(socketFactory, maxPoolSize);
    }

    private QueryOrchestrator queryOrchestrator(MiscConf miscConf) {
        return new QueryOrchestrator(miscConf.getTimeout(), miscConf.getDnsSecMode(), miscConf.getRetries());
    }

    private UpstreamUnitConf upstreamUnitConf(ResolverProvider resolverProvider, ResolverConf resolverConf, MiscConf miscConf, QueryOrchestrator queryOrchestrator) {
        return new UpstreamUnitConf(resolverProvider.getAllResolvers(), resolverConf.getMultiplier(), miscConf.getDnsSecMode(), miscConf.isServeExpired(), miscConf.getTimeout(), queryOrchestrator);
    }

    private Map<String, String> localMappings(List<LocalMapping> localMappings) {
        Map<String, String> mappings = new HashMap<>();
        for (LocalMapping localMapping : localMappings) {
            mappings.put(localMapping.getNormalizedDomain(), localMapping.getIp());
        }
        return mappings;
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
                                                  BlockDecider blockDecider,
                                                  Map<String, String> localMappings,
                                                  CacheManager cacheManager,
                                                  UpstreamUnitConf upstreamUnitConf) {

        return new EngineUnitProvider(executorServiceFactory, blockDecider, localMappings, cacheManager, upstreamUnitConf);
    }

    private PostHandlerFacade postHandlerFacade(PostHandlerProvider provider, ExecutorServiceFactory executorServiceFactory) {
        return new PostHandlerFacade(provider, executorServiceFactory);
    }

    private PostHandlerProvider postHandlerProvider(CacheManager cacheManager, NotificationManager notificationManager, Set<String> urlsToNotify) {
        return new PostHandlerProvider(cacheManager, notificationManager, urlsToNotify);
    }

}
