package com.allanvital.dnsao.dns.processor.recursive.infra;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class GlueRecord {

    private final String ownerName;
    private final String ip;

    public GlueRecord(String ownerName, String ip) {
        this.ownerName = ownerName;
        this.ip = ip;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getIp() {
        return ip;
    }
}
