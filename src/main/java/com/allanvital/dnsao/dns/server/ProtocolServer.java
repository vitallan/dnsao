package com.allanvital.dnsao.dns.server;

import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.infra.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class ProtocolServer {

    protected static final Logger log = LoggerFactory.getLogger(DNS);

    protected int port;
    protected final ExecutorService threadPool;
    protected final QueryProcessorFactory factory;

    protected boolean running = false;
    protected Thread serverThread;

    public ProtocolServer(ExecutorService threadPool, QueryProcessorFactory factory, int port) {
        this.port = port;
        this.factory = factory;
        this.threadPool = threadPool;
    }

    public int getPort() {
        return port;
    }

    protected abstract Thread buildServerThread();
    protected abstract String threadName();

    public void start() {
        serverThread = buildServerThread();
        serverThread.setName(threadName());
        serverThread.start();
        long start = Clock.currentTimeInMillis();
        while (!running) {
            if (Clock.currentTimeInMillis() - start > 3000) {
                throw new IllegalStateException("Server failed to initialize in under 3 seconds, failing");
            }
            Thread.yield();
        }
        log.debug("Started {} on port {}", threadName(), port);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        threadPool.shutdownNow();
        serverThread.interrupt();
        running = false;
        log.debug("Stopped {}", threadName());
    }

    protected boolean hasValidDnsHeader(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }

        int id = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
        int flags = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int qdCount = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        int anCount = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        int nsCount = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
        int arCount = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);

        if (qdCount < 0 || qdCount > 1) {
            return false;
        }

        if (anCount > 0 || nsCount > 0) {
            return false;
        }

        if (arCount < 0 || arCount > 10) {
            return false;
        }

        boolean qr = (flags & 0x8000) != 0;
        if (qr) {
            return false;
        }

        return true;
    }

}