package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Upstream {

    private String host;
    private String path = "/dns-query";
    private String ip;
    private String tlsAuthName;
    private String protocol;
    private int port = 0;

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
        if (this.ip == null) {
            return "9.9.9.9";
        }
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        if (this.ip != null) {
            return this.ip;
        }
        return this.host;
    }

}
