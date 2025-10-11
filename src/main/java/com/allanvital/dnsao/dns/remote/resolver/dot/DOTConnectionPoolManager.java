package com.allanvital.dnsao.dns.remote.resolver.dot;

import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTConnectionPoolManager {

    private final ConcurrentHashMap<String, DOTConnectionPool> pools = new ConcurrentHashMap<>();
    private final SSLSocketFactory socketFactory;
    private final int sizeOfEachPool;

    public DOTConnectionPoolManager(int sizeOfEachPool) {
        this.socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.sizeOfEachPool = sizeOfEachPool;
    }

    public DOTConnectionPool getPoolFor(String upstreamIp, int port, String tlsAuthName) {
        String key = upstreamIp + ":" + port;
        return pools.computeIfAbsent(key, ip -> new DOTConnectionPool(upstreamIp, port, socketFactory, sizeOfEachPool, tlsAuthName));
    }

}