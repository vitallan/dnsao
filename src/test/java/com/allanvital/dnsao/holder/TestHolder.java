package com.allanvital.dnsao.holder;

import com.allanvital.dnsao.TestEventListener;
import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.ConfLoader;
import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.*;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import com.allanvital.dnsao.infra.clock.Clock;
import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.enableTelemetry;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestHolder {

    protected Conf conf;
    protected TestSystemGraphAssembler assembler = new TestSystemGraphAssembler();
    protected TestQueryInfraAssembler queryInfraAssembler;
    protected TestExecutorServiceFactory testExecutorServiceFactory = new TestExecutorServiceFactory();
    protected TestTimeProvider testTimeProvider = TestTimeProvider.getInstance();
    protected DnsServer dnsServer;
    protected FakeServer fakeUpstreamServer;
    protected final String LOCAL = "127.0.0.1";
    protected TestEventListener eventListener;

    protected void fixUpstreamPorts() {
        List<Upstream> upstreams = conf.getResolver().getUpstreams();
        List<Upstream> upstreamsWithCorrectPort = new LinkedList<>();
        for (Upstream upstream : upstreams) {
            upstream.setPort(fakeUpstreamServer.getPort());
            upstreamsWithCorrectPort.add(upstream);
        }
        conf.getResolver().setUpstreams(upstreamsWithCorrectPort);
    }

    protected void startFakeServer() throws ConfException {
        if (conf == null) {
            Assertions.fail("load conf before starting server");
        }
        try {
            fakeUpstreamServer = new FakeUdpServer(0);
            fakeUpstreamServer.start();
            fixUpstreamPorts();
        } catch (Exception e) {
            Assertions.fail("failed dealing with fakeDnsServer: " + e.getMessage());
        }
    }

    protected void safeStartWithPresetConf() throws ConfException {
        setupSslStore();
        testTimeProvider.setNow(Instant.parse("2025-11-08T10:00:00Z").toEpochMilli());
        Clock.setNewTimeProvider(testTimeProvider);
        enableTelemetry(true);
        eventListener = new TestEventListener(testTimeProvider);
        startFakeServer();
        registerOverride(this.testExecutorServiceFactory);
        dnsServer = assembler.assemble(this.conf);
        queryInfraAssembler = assembler.getQueryInfraAssembler();
        dnsServer.start();
    }

    protected void safeStart(String confFile) throws ConfException {
        loadConf(confFile);
        conf.getMisc().setQueryLog(false);
        safeStartWithPresetConf();
    }

    protected void safeStop() throws InterruptedException {
        if (fakeUpstreamServer != null) {
            try {
                fakeUpstreamServer.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (dnsServer != null) {
            dnsServer.stop();
            while (dnsServer.isRunning()) {
                Thread.yield();
            }
            dnsServer = null;
        }
        testExecutorServiceFactory.stopAndRemoveAllExecutors();
        eventListener.reset();
    }

    protected void setupSslStore() {
        System.setProperty("javax.net.ssl.trustStore", Paths.get("src/test/resources/dot-certs/truststore.p12").toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    protected <T> void registerOverride(T... instances) throws ConfException {
        for (T instance : instances) {
            this.assembler.registerOverride(instance);
        }
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
        fakeUpstreamServer.mockResponse(request, response);
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
        resolver.setTimeout(Duration.ofSeconds(3));
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