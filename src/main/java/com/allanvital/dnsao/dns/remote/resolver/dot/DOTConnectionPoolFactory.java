package com.allanvital.dnsao.dns.remote.resolver.dot;

import com.allanvital.dnsao.conf.inner.Upstream;

import javax.net.ssl.SSLSocketFactory;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTConnectionPoolFactory {

    private final SSLSocketFactory socketFactory;
    private final int sizeOfEachPool;

    public DOTConnectionPoolFactory(SSLSocketFactory socketFactory, int sizeOfEachPool) {
        this.socketFactory = socketFactory;
        this.sizeOfEachPool = sizeOfEachPool;
    }

    public DOTConnectionPool build(Upstream upstream) {
        return new DOTConnectionPool(upstream.getIp(), upstream.getPort(), socketFactory, sizeOfEachPool, upstream.getTlsAuthName());
    }

}
