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

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;
import static com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPool.closeQuiet;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTUpstreamResolver implements UpstreamResolver {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final String ip;
    private final String tlsAuthName;
    private final int port;
    private final DOTConnectionPoolManager dotConnectionPoolManager;

    public DOTUpstreamResolver(DOTConnectionPoolManager dotConnectionPoolManager, Upstream upstream) throws CertificateParsingException, IOException {
        log.debug("building DOTResolver for host: {}", upstream.getIp());
        this.dotConnectionPoolManager = dotConnectionPoolManager;
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
        DOTConnectionPool pool = dotConnectionPoolManager.getPoolFor(ip, port, tlsAuthName);
        SSLSocket socket = pool.acquire();
        try {
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
            socket.close();
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

    @Override
    public Message send(Message query) throws IOException {
        DOTConnectionPool pool = dotConnectionPoolManager.getPoolFor(ip, port, tlsAuthName);
        SSLSocket socket = pool.acquire();
        boolean ok = false;
        try {
            try {
                Message resp = sendOnSocket(socket, query);
                ok = true;
                return resp;
            } catch (IOException e) {
                // simple retry
                socket = pool.acquire();
                Message resp = sendOnSocket(socket, query);
                ok = true;
                return resp;
            }
        } finally {
            if (ok) pool.release(socket); else closeQuiet(socket);
        }
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