package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.cache.CacheEntryFactory;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.processor.engine.pojo.UpstreamUnitConf;
import com.allanvital.dnsao.dns.processor.engine.unit.upstream.QueryOrchestrator;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.UpstreamThreadPoolExecutor;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class UpstreamUnitTest {

    @Test
    public void usesInjectedSharedExecutor() throws Exception {
        ExecutorServiceFactory factory = new ExecutorServiceFactory();
        try (UpstreamThreadPoolExecutor upstreamPool = new UpstreamThreadPoolExecutor(factory, 2, 10)) {
            AtomicReference<ExecutorService> seenExecutor = new AtomicReference<>();

            ResolverProvider provider = new ResolverProvider() {
                @Override
                public List<UpstreamResolver> getAllResolvers() {
                    return List.of();
                }

                @Override
                public List<UpstreamResolver> getResolversToUse() {
                    return List.of();
                }
            };

            UpstreamUnit unit = getUpstreamUnit(seenExecutor, provider, upstreamPool);

            Message req = MessageHelper.buildARequest("example.com");
            DnsQueryRequest request = new DnsQueryRequest(InetAddress.getLoopbackAddress());
            request.setOriginalRequest(req);
            request.setRequest(req);
            request.setIsLocalQuery(false);
            unit.innerProcess(request);

            assertSame(upstreamPool.executor(), seenExecutor.get());
        }
    }

    @NotNull
    private static UpstreamUnit getUpstreamUnit(AtomicReference<ExecutorService> seenExecutor, ResolverProvider provider, UpstreamThreadPoolExecutor upstreamPool) {
        QueryOrchestrator orchestrator = new QueryOrchestrator(1, DNSSecMode.SIMPLE, 1, new CacheEntryFactory()) {
            @Override
            public DnsQueryResult query(ExecutorService executorService, DnsQueryRequest request, List<UpstreamResolver> resolvers) {
                seenExecutor.set(executorService);
                Message req = request.getRequest();
                Message resp = MessageHelper.buildAResponse(req, "10.10.10.10", 2);
                UpstreamResolver resolver = new UpstreamResolver() {
                    @Override
                    public String getIp() {
                        return "127.0.0.1";
                    }

                    @Override
                    public int getPort() {
                        return 53;
                    }

                    @Override
                    public Message send(Message query) {
                        return resp;
                    }

                    @Override
                    public String name() {
                        return "test";
                    }
                };
                return new DnsQueryResult(resp, resolver);
            }
        };

        UpstreamUnitConf conf = new UpstreamUnitConf(
                DNSSecMode.SIMPLE,
                false,
                1,
                orchestrator,
                provider
        );

        UpstreamUnit unit = new UpstreamUnit(upstreamPool, conf);
        return unit;
    }
}
