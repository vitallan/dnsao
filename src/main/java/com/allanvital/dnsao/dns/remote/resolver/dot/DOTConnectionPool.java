package com.allanvital.dnsao.dns.remote.resolver.dot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTConnectionPool {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private static final int SOCKET_TIMEOUT = 5000;
    private int ACQUIRE_TIMEOUT = 2000;
    private static final long IDLE_TTL_NANOS = Duration.ofSeconds(20).toNanos();
    private final ReentrantLock LOCK = new ReentrantLock(true);

    private final Queue<DOTChannel> pool = new LinkedList<>();

    private final Semaphore permits;
    private final String ip;
    private final int port;
    private final SSLSocketFactory socketFactory;
    private final String tlsAuthName;

    public DOTConnectionPool(String ip, int port, SSLSocketFactory socketFactory, int poolSize, String tlsAuthName) {
        this.ip = ip;
        this.port = port;
        this.socketFactory = socketFactory;
        this.permits = new Semaphore(poolSize, true);
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

    public SSLSocket acquire() throws IOException, TimeoutException {
        try {
            if (!permits.tryAcquire(ACQUIRE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Timed out waiting for a free connection");
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        boolean success = false;
        try {
            DOTChannel dotChannel = null;
            LOCK.lock();
            try {
                while (!pool.isEmpty()) {
                    DOTChannel candidate = pool.poll();
                    if (isHealthy(candidate.getSocket()) && !isStale(candidate)) {
                        dotChannel = candidate;
                        break;
                    } else {
                        closeQuiet(candidate.getSocket());
                    }
                }
            } finally {
                LOCK.unlock();
            }
            if (dotChannel == null) {
                dotChannel = new DOTChannel(createConnection(), System.nanoTime());
            }
            success = true;
            return dotChannel.getSocket();
        } finally {
            if (!success) {
                permits.release();
            }
        }
    }

    public void release(SSLSocket socket) {
        release(socket, false);
    }

    public void release(SSLSocket socket, boolean knownBadSocket) {
        if (socket == null) {
            return;
        }
        boolean returnedToPool = false;
        if (!knownBadSocket) {
            boolean healthy = isHealthy(socket);
            if (healthy) {
                LOCK.lock();
                try {
                    returnedToPool = pool.offer(new DOTChannel(socket, System.nanoTime()));
                } finally {
                    LOCK.unlock();
                }
            }
        }
        if (!returnedToPool) {
            closeQuiet(socket);
        }
        permits.release();
    }

    private boolean isStale(DOTChannel channel) {
        long idle = System.nanoTime() - channel.getLastUsedNanos();
        return idle > IDLE_TTL_NANOS;
    }

    private boolean isHealthy(SSLSocket socket) {
        return !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown();
    }

    public int getPoolSize() {
        return this.pool.size();
    }

    public void setAcquireTimeout(int timeout) {
        this.ACQUIRE_TIMEOUT = timeout;
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