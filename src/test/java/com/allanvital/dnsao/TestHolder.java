package com.allanvital.dnsao;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.ConfLoader;
import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.dns.processor.QueryProcessorDependencies;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.QueryInfraAssembler;
import com.allanvital.dnsao.graph.TestExecutorServiceFactory;
import com.allanvital.dnsao.graph.TestSystemGraphAssembler;
import com.allanvital.dnsao.graph.TestTimeProvider;
import com.allanvital.dnsao.graph.bean.FakeDnsServer;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.infra.clock.Clock;
import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestHolder {

    protected Conf conf;
    protected TestSystemGraphAssembler assembler = new TestSystemGraphAssembler();
    protected TestExecutorServiceFactory testExecutorServiceFactory = new TestExecutorServiceFactory();
    protected TestTimeProvider testTimeProvider;
    protected DnsServer dnsServer;
    protected FakeDnsServer fakeDnsServer;
    protected final String LOCAL = "127.0.0.1";
    protected TestEventListener eventListener;

    protected void startFakeDnsServer() throws ConfException {
        if (conf == null) {
            Assertions.fail("load conf before starting server");
        }
        try {
            fakeDnsServer = new FakeDnsServer(0);
            fakeDnsServer.start();

            List<Upstream> upstreams = conf.getResolver().getUpstreams();
            List<Upstream> upstreamsWithCorrectPort = new LinkedList<>();
            for (Upstream upstream : upstreams) {
                upstream.setPort(fakeDnsServer.getPort());
                upstreamsWithCorrectPort.add(upstream);
            }
            conf.getResolver().setUpstreams(upstreamsWithCorrectPort);
        } catch (SocketException e) {
            Assertions.fail("failed dealing with fakeDnsServer: " + e.getMessage());
        }
    }

    protected void safeStartWithPresetConf() throws ConfException {
        testTimeProvider = new TestTimeProvider(System.currentTimeMillis());
        eventListener = new TestEventListener(testTimeProvider);
        Clock.setNewTimeProvider(testTimeProvider);
        startFakeDnsServer();
        registerOverride(this.testExecutorServiceFactory);
        dnsServer = assembler.assemble(this.conf);
        dnsServer.start();
    }

    protected void safeStart(String confFile) throws ConfException {
        loadConf(confFile);
        safeStartWithPresetConf();
    }

    protected void safeStop() throws InterruptedException {
        if (fakeDnsServer != null) {
            fakeDnsServer.stop();
        }
        if (dnsServer != null) {
            dnsServer.stop();
            testExecutorServiceFactory.stopAndRemoveAllExecutors();
            while (dnsServer.isRunning()) {
                Thread.yield();
            }
        }
        eventListener.reset();
    }

    protected <T> void registerOverride(T... instances) throws ConfException {
        for (T instance : instances) {
            this.assembler.getOverrideRegistry().registerOverride(instance);
        }
    }

    protected QueryProcessorDependencies getQueryProcessorDependencies(CacheManager cacheManager) throws ConfException {
        if (conf == null) {
            Assertions.fail("load conf before getting QueryProcessorDependencies");
        }
        QueryInfraAssembler queryInfraAssembler = new QueryInfraAssembler(assembler.getOverrideRegistry());
        return queryInfraAssembler.assemble(conf.getResolver(), conf.getMisc(), cacheManager, null);
    }

    protected InetAddress getClient() {
        try {
            return InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            Assertions.fail("failed building client: " + e.getMessage());
            return null;
        }
    }

    protected void loadConf(String configYml, boolean f) throws ConfException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(configYml);
        conf = ConfLoader.load(input);
    }

    protected void loadConf(String configYml) throws ConfException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(configYml);
        conf = ConfLoader.load(input);
    }

    protected void prepareSimpleMockResponse(String domain, String ip) {
        this.prepareSimpleMockResponse(domain, ip, 300);
    }

    protected Message prepareSimpleMockResponse(String domain, String ip, long ttlInSeconds) {
        Message request = MessageHelper.buildARequest(domain);
        Message response = MessageHelper.buildAResponse(request, ip, ttlInSeconds);
        fakeDnsServer.mockResponse(request, response);
        return response;
    }

    protected SimpleResolver buildResolver(int udtPort) throws UnknownHostException {
        SimpleResolver resolver = new SimpleResolver(LOCAL);
        resolver.setPort(udtPort);
        resolver.setTCP(false);
        return resolver;
    }

    protected Message doRequest(SimpleResolver resolver, String domain) throws IOException {
        Message request = MessageHelper.buildARequest(domain);
        resolver.setTimeout(Duration.ofSeconds(10));
        return resolver.send(request);
    }

    protected Message executeRequestOnOwnServer(DnsServer realServer, String domain, boolean tcp) throws IOException {
        SimpleResolver resolver = new SimpleResolver(LOCAL);
        if (tcp) {
            resolver.setPort(realServer.getTcpPort());
        } else {
            resolver.setPort(realServer.getUdpPort());
        }
        resolver.setTCP(tcp);

        Message request = MessageHelper.buildARequest(domain);
        return resolver.send(request);
    }

    public static long t(String isoInstant) {
        return Instant.parse(isoInstant).toEpochMilli();
    }

}