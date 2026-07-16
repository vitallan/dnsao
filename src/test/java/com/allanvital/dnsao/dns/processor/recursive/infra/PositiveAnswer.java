package com.allanvital.dnsao.dns.processor.recursive.infra;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PositiveAnswer {

    private final int type;
    private final String ownerName;
    private final String value;
    private final long ttl;

    public PositiveAnswer(int type, String ownerName, String value, long ttl) {
        this.type = type;
        this.ownerName = normalize(ownerName);
        this.value = value;
        this.ttl = ttl;
    }

    public static PositiveAnswer a(String ownerName, String value, long ttl) {
        return new PositiveAnswer(org.xbill.DNS.Type.A, ownerName, value, ttl);
    }

    public static PositiveAnswer cname(String ownerName, String target, long ttl) {
        return new PositiveAnswer(org.xbill.DNS.Type.CNAME, ownerName, target, ttl);
    }

    public int getType() {
        return type;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getValue() {
        return value;
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
