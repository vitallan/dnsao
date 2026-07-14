package com.allanvital.dnsao.dns.processor.recursive.infra;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ReferralAnswer {

    private final String delegatedZone;
    private final List<String> nameservers = new ArrayList<>();
    private final List<GlueRecord> glueRecords = new ArrayList<>();
    private final long ttl;

    public ReferralAnswer(String delegatedZone) {
        this(delegatedZone, 300L);
    }

    public ReferralAnswer(String delegatedZone, long ttl) {
        this.delegatedZone = normalize(delegatedZone);
        this.ttl = ttl;
    }

    public void addNameserver(String nameserver) {
        nameservers.add(normalize(nameserver));
    }

    public void addGlueA(String ownerName, String ip) {
        glueRecords.add(new GlueRecord(normalize(ownerName), ip));
    }

    public String getDelegatedZone() {
        return delegatedZone;
    }

    public List<String> getNameservers() {
        return nameservers;
    }

    public List<GlueRecord> getGlueRecords() {
        return glueRecords;
    }

    public long getTtl() {
        return ttl;
    }

    private static String normalize(String value) {
        if (value.endsWith(".")) {
            return value;
        }
        return value + ".";
    }
}
