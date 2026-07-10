package com.allanvital.dnsao.dns;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.conf.MutableState;
import com.allanvital.dnsao.conf.inner.ServerConf;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.server.ProtocolServer;
import com.allanvital.dnsao.dns.server.TcpServer;
import com.allanvital.dnsao.dns.server.UdpServer;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import com.allanvital.dnsao.web.json.JsonBuilder;
import com.allanvital.dnsao.web.stats.StatsCollector;
import com.allanvital.dnsao.web.WebServer;
import com.allanvital.dnsao.dns.remote.UpstreamThreadPoolExecutor;

import java.util.concurrent.ExecutorService;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServer {


    private final int port;
    private volatile boolean running = false;
    private final ExecutorService udpThreadPool;
    private final ExecutorService tcpThreadPool;
    private final ProtocolServer udpServer;
    private final ProtocolServer tcpServer;
    private final WebServer webServer;
    private final StatsCollector statsCollector;
    private final UpstreamThreadPoolExecutor upstreamThreadPoolExecutor;
    private final MutableState mutableState;
    private final String authPass;

    public DnsServer(ServerConf conf,
                     QueryProcessorFactory factory,
                     ExecutorServiceFactory executorServiceFactory,
                     StatsCollector statsCollector,
                     UpstreamThreadPoolExecutor upstreamThreadPoolExecutor,
                     JsonBuilder jsonBuilder,
                     MutableState mutableState,
                     String authPass) {
        this.port = conf.getPort();
        this.statsCollector = statsCollector;
        this.upstreamThreadPoolExecutor = upstreamThreadPoolExecutor;
        this.mutableState = mutableState;
        this.authPass = authPass;
        this.udpThreadPool = executorServiceFactory.buildExecutor("udp", conf.getUdpThreadPool());
        this.tcpThreadPool = executorServiceFactory.buildExecutor("tcp", conf.getTcpThreadPool());
        udpServer = new UdpServer(udpThreadPool, factory, port);
        tcpServer = new TcpServer(tcpThreadPool, factory, port);
        webServer = new WebServer(conf.getWebPort(), factory, conf.getHttpThreadPool(), jsonBuilder, mutableState, authPass);
    }

    public int getUdpPort() {
        return udpServer.getPort();
    }

    public int getTcpPort() {
        return tcpServer.getPort();
    }

    public int getHttpPort() {
        return webServer.getPort();
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            return;
        }
        udpServer.start();
        tcpServer.start();
        webServer.start();
        running = true;
        Log.DNS.info("DNS server now running on ports {} for udp and {} for tcp and accepting connections", getUdpPort(), getTcpPort());
    }

    public void stop() {
        if (udpServer != null && udpServer.isRunning()) {
            udpServer.stop();
        }
        if (tcpServer != null && tcpServer.isRunning()) {
            tcpServer.stop();
        }
        if (webServer != null && webServer.isRunning()) {
            webServer.stop();
        }
        if (statsCollector instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                Log.DNS.warn("failed closing stats collector", e);
            }
        }
        if (upstreamThreadPoolExecutor != null) {
            try {
                upstreamThreadPoolExecutor.close();
            } catch (Exception e) {
                Log.DNS.warn("failed closing upstream executor", e);
            }
        }
        running = false;
        Log.DNS.info("DNS server stopped");
    }

}
