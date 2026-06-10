package com.allanvital.dnsao.dns.recursive;

public class NameServerAddress {

    private final String ip;
    private final int port;

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
