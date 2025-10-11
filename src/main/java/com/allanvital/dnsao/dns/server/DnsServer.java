package com.allanvital.dnsao.dns.server;

import com.allanvital.dnsao.conf.inner.ServerConf;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.server.internal.ProtocolServer;
import com.allanvital.dnsao.dns.server.internal.TcpServer;
import com.allanvital.dnsao.dns.server.internal.UdpServer;
import com.allanvital.dnsao.utils.ThreadShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.AppLoggers.DNS;

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

    public DnsServer(ServerConf conf, QueryProcessorFactory factory) {
        this.port = conf.getPort();
        this.udpThreadPool = ThreadShop.buildExecutor("udp", conf.getUdpThreadPool());
        this.tcpThreadPool = ThreadShop.buildExecutor("tcp", conf.getTcpThreadPool());
        udpServer = new UdpServer(udpThreadPool, factory, port);
        tcpServer = new TcpServer(tcpThreadPool, factory, port);
    }

    public int getUdpPort() {
        return udpServer.getPort();
    }

    public int getTcpPort() {
        return tcpServer.getPort();
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
        running = false;
        log.info("DNS server stopped");
    }

}