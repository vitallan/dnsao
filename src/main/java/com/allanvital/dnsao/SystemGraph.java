package com.allanvital.dnsao;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.RewarmCacheManager;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.dns.server.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.utils.DownloadUtils;
import com.allanvital.dnsao.utils.ExceptionUtils;
import com.allanvital.dnsao.utils.FileUtils;
import com.allanvital.dnsao.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

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
        RewarmCacheManager rewarmCacheManager = rewarmCacheManager(cacheConf, resolverFactory);
        CacheManager cacheManager = cacheManager(cacheConf, rewarmCacheManager);

        Set<String> blockList = blockList(resolverConf.getBlocklists());
        Map<String, String> locaMappings = localMappings(resolverConf.getLocalMappings());

        QueryProcessorFactory factory = queryProcessorFactory(resolverFactory, cacheManager, blockList, locaMappings, resolverConf.getMultiplier());

        dnsServer = dnsServer(serverConf, factory);
        webServer = webServer(serverConf.getWebPort());
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

    private WebServer webServer(int webPort) {
        return new WebServer(webPort);
    }

    private RewarmCacheManager rewarmCacheManager(CacheConf cacheConf, ResolverFactory factory) {
        return new RewarmCacheManager(cacheConf, factory, 1000);
    }

    private CacheManager cacheManager(CacheConf cacheConf, RewarmCacheManager rewarmCacheManager) {
        return new CacheManager(cacheConf, rewarmCacheManager);
    }

    private QueryProcessorFactory queryProcessorFactory(ResolverFactory resolverFactory, CacheManager cacheManager, Set<String> blockList, Map<String, String> localMappings, int multiplier) {
        return new QueryProcessorFactory(resolverFactory.getAllResolvers(), cacheManager, blockList, localMappings, multiplier);
    }

    private DOTConnectionPoolManager dotConnectionPoolManager(int tlsPoolSize) {
        return new DOTConnectionPoolManager(tlsPoolSize);
    }

    private Set<String> blockList(List<String> blockList) {
        Set<String> blockedDomains = new TreeSet<>();
        if (blockList != null && !blockList.isEmpty()) {
            for (String blockUrl : blockList) {
                try {
                    DownloadUtils.downloadToTemp(blockUrl);
                } catch (IOException | InterruptedException e) {
                    log.error("unable do download " + blockUrl + ". Error was " + ExceptionUtils.findRootCause(e).getMessage());
                }
            }
            try {
                blockedDomains = FileUtils.getBlockedDomains();
            } catch (IOException e) {
                log.error("unable read blocklist files . Error was " + ExceptionUtils.findRootCause(e).getMessage());
            }
        }
        return blockedDomains;
    }

    private Map<String, String> localMappings(List<LocalMapping> localMappings) {
        Map<String, String> mappings = new HashMap<>();
        for (LocalMapping localMapping : localMappings) {
            mappings.put(localMapping.getNormalizedDomain(), localMapping.getIp());
        }
        return mappings;
    }

}
