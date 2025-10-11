package com.allanvital.dnsao.dns.remote.resolver.dot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final String ip;
    private final int port;
    private final SSLSocketFactory socketFactory;
    private final BlockingQueue<DOTChannel> pool;
    private final String tlsAuthName;
    private final int SOCKET_TIMEOUT = 5000;
    private final long IDLE_TTL_NANOS = Duration.ofSeconds(20).toNanos();

    public DOTConnectionPool(String ip, int port, SSLSocketFactory socketFactory, int poolSize, String tlsAuthName) {
        this.ip = ip;
        this.port = port;
        this.socketFactory = socketFactory;
        this.pool = new LinkedBlockingQueue<>(poolSize);
        this.tlsAuthName = tlsAuthName;
    }

    private SSLSocket createConnection() throws IOException {
        log.debug("creating new connection to {}", tlsAuthName);
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(ip, port);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});

        SSLParameters sslParams = new SSLParameters();
        sslParams.setEndpointIdentificationAlgorithm("HTTPS");
        sslParams.setServerNames(List.of(new SNIHostName(tlsAuthName)));

        socket.setSSLParameters(sslParams);
        socket.startHandshake();
        return socket;
    }

    public SSLSocket acquire() throws IOException {
        DOTChannel channel = pool.poll();
        if (channel == null) {
            return createConnection();
        }
        SSLSocket socket = channel.getSocket();
        if (!isHealthy(socket) || isStale(channel)) {
            log.debug("connection to {} found, but is stale={} healthy={}, creating a new one", tlsAuthName, isStale(channel), isHealthy(socket));
            closeQuiet(socket);
            return createConnection();
        }
        return socket;
    }

    public void release(SSLSocket socket) {
        if (socket == null || !isHealthy(socket)) {
            log.debug("connection to " + tlsAuthName + " freed, but is unhealthy, closing");
            closeQuiet(socket);
            return;
        }
        DOTChannel channel = new DOTChannel(socket, System.nanoTime());
        if (!pool.offer(channel)) {
            log.debug("connection to " + tlsAuthName + " was refused on pool");
            closeQuiet(socket);
        }
    }

    private boolean isStale(DOTChannel channel) {
        long idle = System.nanoTime() - channel.getLastUsedNanos();
        return idle > IDLE_TTL_NANOS;
    }

    private boolean isHealthy(SSLSocket socket) {
        return !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }

    public static void closeQuiet(SSLSocket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignore) {
                log.trace("ignored exception: {}", ignore.getMessage());
            }
        }
    }

}