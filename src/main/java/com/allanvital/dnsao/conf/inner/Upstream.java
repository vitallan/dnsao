package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Upstream {

    private String ip;
    private String tlsAuthName;
    private String protocol;
    private int port;

    public String getTlsAuthName() {
        return tlsAuthName;
    }

    public void setTlsAuthName(String tlsAuthName) {
        this.tlsAuthName = tlsAuthName;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return this.ip;
    }

}
