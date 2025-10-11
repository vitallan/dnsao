package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LocalMapping {

    private String domain;
    private String ip;

    public String getNormalizedDomain() {
        if (domain == null) {
            return null;
        }
        String normalizedDomain = domain.toLowerCase();
        if (!normalizedDomain.endsWith(".")) {
            normalizedDomain = normalizedDomain + ".";
        }
        return normalizedDomain;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

}