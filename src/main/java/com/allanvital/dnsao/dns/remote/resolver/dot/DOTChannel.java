package com.allanvital.dnsao.dns.remote.resolver.dot;

import javax.net.ssl.SSLSocket;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOTChannel {

    private final SSLSocket socket;
    private final long lastUsedNanos;

    public DOTChannel(SSLSocket s, long lastUsedNanos) {
        this.socket = s;
        this.lastUsedNanos = lastUsedNanos;
    }

    public SSLSocket getSocket() {
        return socket;
    }

    public long getLastUsedNanos() {
        return lastUsedNanos;
    }

}