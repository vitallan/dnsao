package com.allanvital.dnsao;

import com.allanvital.dnsao.block.BlockListProvider;
import com.allanvital.dnsao.block.DownloadFileHandler;
import com.allanvital.dnsao.block.FileHandler;
import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.RewarmScheduler;
import com.allanvital.dnsao.cache.rewarm.RewarmWorker;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.local.LocalResolver;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.dns.server.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.utils.DownloadUtils;
import com.allanvital.dnsao.utils.ThreadShop;
import com.allanvital.dnsao.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SystemGraph {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private DnsServer dnsServer;
    private WebServer webServer;

    public SystemGraph (Conf conf) throws ConfException {
        CacheConf cacheConf = conf.getCache();
        ResolverConf resolverConf = conf.getResolver();
        ServerConf serverConf = conf.getServer();

        DOTConnectionPoolManager poolManager = dotConnectionPoolManager(resolverConf.getTlsPoolSize());
        ResolverFactory resolverFactory = resolverFactory(poolManager, resolverConf.getUpstreams());
        RewarmScheduler rewarmScheduler = rewarmScheduler(20_000);
        CacheManager cacheManager = cacheManager(cacheConf, rewarmScheduler);

        Map<String, String> locaMappings = localMappings(resolverConf.getLocalMappings());

        FileHandler fileHandler = fileHandler();

        BlockListProvider blockListProvider = blockListProvider(resolverConf.getAllowLists(), resolverConf.getBlocklists(), fileHandler);

        LocalResolver localResolver = localResolver(blockListProvider, locaMappings);

        QueryProcessorFactory factory = queryProcessorFactory(resolverFactory, cacheManager, localResolver, resolverConf);

        ExecutorService rewarmService = rewarmWorkerExecutor();
        scheduleRewarmWorker(rewarmService, cacheConf, rewarmScheduler, cacheManager, factory);

        dnsServer = dnsServer(serverConf, factory);
        webServer = webServer(serverConf.getWebPort(), factory, serverConf.getHttpThreadPool());
    }

    private LocalResolver localResolver(BlockListProvider blockListProvider, Map<String, String> locaMappings) {
        return new LocalResolver(blockListProvider, locaMappings);
    }

    public void start() {
        dnsServer.start();
        webServer.start();
    }

    public void stop() {
        if (dnsServer != null) {
            dnsServer.stop();
        }
        if (webServer != null) {
            webServer.stop();
        }
    }

    public DnsServer getDnsServer() {
        return dnsServer;
    }

    private DnsServer dnsServer(ServerConf conf, QueryProcessorFactory queryProcessorFactory) {
        return new DnsServer(conf, queryProcessorFactory);
    }

    private ResolverFactory resolverFactory(DOTConnectionPoolManager dotConnectionPoolManager, List<Upstream> upstreams) {
        return new ResolverFactory(dotConnectionPoolManager, upstreams);
    }

    private WebServer webServer(int webPort, QueryProcessorFactory queryProcessorFactory, int httpThreadPool) {
        return new WebServer(webPort, queryProcessorFactory, httpThreadPool);
    }

    public static RewarmWorker scheduleRewarmWorker(ExecutorService executorService, CacheConf cacheConf, RewarmScheduler rewarmScheduler, CacheManager cacheManager, QueryProcessorFactory queryProcessorFactory) {
        if (cacheConf.isRewarm()) {
            RewarmWorker rewarmWorker = new RewarmWorker(rewarmScheduler, cacheManager, queryProcessorFactory, cacheConf.getMaxRewarmCount());
            executorService.submit(rewarmWorker);
            return rewarmWorker;
        }
        return null;
    }

    private ExecutorService rewarmWorkerExecutor() {
        return ThreadShop.buildExecutor("rewarm", 1);
    }

    private CacheManager cacheManager(CacheConf cacheConf, RewarmScheduler rewarmScheduler) {
        return new CacheManager(cacheConf, rewarmScheduler);
    }

    private RewarmScheduler rewarmScheduler(long timeBeforeTtlToTriggerRewarm) {
        return new RewarmScheduler(timeBeforeTtlToTriggerRewarm);
    }

    private QueryProcessorFactory queryProcessorFactory(ResolverFactory resolverFactory, CacheManager cacheManager, LocalResolver localResolver, ResolverConf resolverConf) {
        return new QueryProcessorFactory(resolverFactory.getAllResolvers(), cacheManager, localResolver, resolverConf);
    }

    public BlockListProvider blockListProvider(List<String> blockList, List<String> allowList, FileHandler fileHandler) {
        return new BlockListProvider(blockList, allowList, fileHandler);
    }

    private DOTConnectionPoolManager dotConnectionPoolManager(int tlsPoolSize) {
        return new DOTConnectionPoolManager(tlsPoolSize);
    }

    private FileHandler fileHandler() throws ConfException {
        Path workDir = null;
        try {
            workDir = DownloadUtils.getAppDir();
            return new DownloadFileHandler(workDir);
        } catch (IOException e) {
            throw new ConfException(e.getMessage());
        }
    }

    private Map<String, String> localMappings(List<LocalMapping> localMappings) {
        Map<String, String> mappings = new HashMap<>();
        for (LocalMapping localMapping : localMappings) {
            mappings.put(localMapping.getNormalizedDomain(), localMapping.getIp());
        }
        return mappings;
    }

}
