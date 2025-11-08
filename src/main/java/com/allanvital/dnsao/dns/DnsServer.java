package com.allanvital.dnsao.dns;

import com.allanvital.dnsao.conf.inner.ServerConf;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.server.ProtocolServer;
import com.allanvital.dnsao.dns.server.TcpServer;
import com.allanvital.dnsao.dns.server.UdpServer;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import com.allanvital.dnsao.web.StatsCollector;
import com.allanvital.dnsao.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsServer {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    private final int port;
    private volatile boolean running = false;
    private final ExecutorService udpThreadPool;
    private final ExecutorService tcpThreadPool;
    private final ProtocolServer udpServer;
    private final ProtocolServer tcpServer;
    private final WebServer webServer;

    public DnsServer(ServerConf conf, QueryProcessorFactory factory, ExecutorServiceFactory executorServiceFactory, StatsCollector statsCollector) {
        this.port = conf.getPort();
        this.udpThreadPool = executorServiceFactory.buildExecutor("udp", conf.getUdpThreadPool());
        this.tcpThreadPool = executorServiceFactory.buildExecutor("tcp", conf.getTcpThreadPool());
        udpServer = new UdpServer(udpThreadPool, factory, port);
        tcpServer = new TcpServer(tcpThreadPool, factory, port);
        webServer = new WebServer(conf.getWebPort(), factory, conf.getHttpThreadPool(), statsCollector);
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
        log.info("DNS server now running on ports {} for udp and {} for tcp and accepting connections", getUdpPort(), getTcpPort());
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
        running = false;
        log.info("DNS server stopped");
    }

}