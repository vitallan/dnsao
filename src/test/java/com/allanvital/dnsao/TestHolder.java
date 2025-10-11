package com.allanvital.dnsao;

import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.conf.ConfLoader;
import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.server.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.helper.FakeDnsServer;
import com.allanvital.dnsao.helper.MessageUtils;
import org.junit.jupiter.api.Assertions;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestHolder {

    protected Conf conf;
    protected SystemGraph systemGraph;
    protected FakeDnsServer fakeDnsServer;
    protected final String LOCAL = "127.0.0.1";
    protected TestEventListener eventListener = new TestEventListener();

    protected void startFakeDnsServer() {
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

    protected void stopFakeDnsServer() {
        if (fakeDnsServer != null) {
            fakeDnsServer.stop();
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

    protected void loadConf(String configYml, boolean startGraph) throws ConfException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(configYml);
        conf = ConfLoader.load(input);
        if (startGraph) {
            systemGraph = new SystemGraph(conf);
        }
    }

    protected void prepareSimpleMockResponse(String domain, String ip) {
        this.prepareSimpleMockResponse(domain, ip, 300);
    }

    protected Message prepareSimpleMockResponse(String domain, String ip, long ttlInSeconds) {
        Message request = MessageUtils.buildARequest(domain);
        Message response = MessageUtils.buildAResponse(request, ip, ttlInSeconds);
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
        Message request = MessageUtils.buildARequest(domain);
        return resolver.send(request);
    }

    protected Message executeRequestOnDnsao(DnsServer realServer, String domain, boolean tcp) throws IOException {
        SimpleResolver resolver = new SimpleResolver(LOCAL);
        if (tcp) {
            resolver.setPort(realServer.getTcpPort());
        } else {
            resolver.setPort(realServer.getUdpPort());
        }
        resolver.setTCP(tcp);

        Message request = MessageUtils.buildARequest(domain);
        return resolver.send(request);
    }

    public static long t(String isoInstant) {
        return Instant.parse(isoInstant).toEpochMilli();
    }

}