package com.allanvital.dnsao.dns.recursive;

import static com.allanvital.dnsao.Constants.DEFAULT_DNS_PORT;

public class NameServerAddress {

    private final String ip;
    private final int port;

    public NameServerAddress(String ip) {
        this(ip, DEFAULT_DNS_PORT);
    }

    public NameServerAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String ip() {
        return ip;
    }

    public int port() {
        return port;
    }

}
