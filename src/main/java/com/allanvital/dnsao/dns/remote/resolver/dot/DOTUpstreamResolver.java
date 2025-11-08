package com.allanvital.dnsao.dns.remote.resolver.dot;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTUpstreamResolver implements UpstreamResolver {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final String ip;
    private final String tlsAuthName;
    private final int port;
    private final DOTConnectionPool pool;

    public DOTUpstreamResolver(DOTConnectionPool pool, Upstream upstream) throws CertificateParsingException, IOException {
        log.debug("building DOTResolver for host: {}", upstream.getIp());
        this.pool = pool;
        this.ip = upstream.getIp();
        this.tlsAuthName = upstream.getTlsAuthName();
        if (upstream.getPort() == 0) {
            this.port = 853;
        } else {
            this.port = upstream.getPort();
        }
        verifyTlsAuthName();
        log.debug("DOTResolver for host {} built", upstream.getIp());
    }

    private void verifyTlsAuthName() throws IOException, CertificateParsingException {
        SSLSocket socket = null;
        try {
            socket = forceAcquire();
            synchronized (socket) {
                SSLSession session = socket.getSession();
                Certificate[] certs = session.getPeerCertificates();
                X509Certificate cert = (X509Certificate) certs[0];

                Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
                boolean matched = false;
                if (altNames != null) {
                    for (List<?> entry : altNames) {
                        if (entry.get(0).equals(2)) { // type 2 is dns
                            String san = (String) entry.get(1);
                            if (tlsAuthName.equalsIgnoreCase(san)) {
                                matched = true;
                                break;
                            }
                        }
                    }
                }

                if (!matched) {
                    throw new SSLPeerUnverifiedException("Upstream certificate does not match expected tls_auth_name: " + tlsAuthName);
                }
            }
        } finally {
            DOTConnectionPool.closeQuiet(socket);
            pool.release(socket);
        }

    }

    @Override
    public String getIp() {
        return this.ip;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    public String getTlsAuthName() {
        return this.tlsAuthName;
    }

    public DOTConnectionPool getPool() {
        return pool;
    }

    @Override
    public Message send(Message query) throws IOException {
        SSLSocket socket = forceAcquire();
        Message response = null;
        int retries = 0;
        int maxInternalRetries = 3;
        try {
            while (response == null && maxInternalRetries > retries) {
                try {
                    response = sendOnSocket(socket, query);
                } catch (IOException e) {
                    retries++;
                    log.debug("failed to send query on TLS socket: {} for {}. Retrying", e.getMessage(), this.tlsAuthName);
                    pool.release(socket, true);
                    socket = forceAcquire();
                }
            }
        } finally {
            pool.release(socket);
        }
        return response;
    }

    private SSLSocket forceAcquire() throws IOException {
        SSLSocket socket = null;
        while (socket == null) {
            try {
                socket = pool.acquire();
            } catch (IOException | TimeoutException e) {
                log.warn("it was not possible to acquire a sslSocket, error was {}. Retrying...", e.getMessage());
                throw new IOException(e);
            }
        }
        return socket;
    }

    private Message sendOnSocket(SSLSocket socket, Message query) throws IOException {
        synchronized (socket) {
            OutputStream out = socket.getOutputStream();
            byte[] data = query.toWire();
            int len = data.length;
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
            out.write(data);
            out.flush();

            DataInputStream din = new DataInputStream(socket.getInputStream());
            int b1 = din.read();
            int b2 = din.read();
            if (b1 == -1 || b2 == -1) throw new IOException("EOF before response length");
            int respLen = (b1 << 8) | b2;

            byte[] resp = new byte[respLen];
            din.readFully(resp);
            return new Message(resp);
        }
    }

    @Override
    public String toString() {
        return this.name();
    }

}