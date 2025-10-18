package com.allanvital.dnsao.cache.pojo;

import org.xbill.DNS.Message;

import java.util.Objects;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsCacheEntry {

    private final Message response;
    private volatile long expiryTime;
    private long configuredTtlInSeconds;
    private int rewarmCount = 0;

    public DnsCacheEntry(Message response, Long ttlInSeconds) {
        this.response = response;
        if (ttlInSeconds == null) {
            configuredTtlInSeconds = 30;
        } else {
            configuredTtlInSeconds = ttlInSeconds;
        }
        this.expiryTime = System.currentTimeMillis() + (configuredTtlInSeconds * 1000);
    }

    public DnsCacheEntry(Message response, Long ttlInSeconds, int rewarmCount) {
        this(response, ttlInSeconds);
        this.rewarmCount = rewarmCount;
    }

    public Message getResponse() {
        return response;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    public long getRemainingTtlMillis() {
        return expiryTime - System.currentTimeMillis();
    }

    public int getRewarmCount() {
        return rewarmCount;
    }

    public void setRewarmCount(int rewarmCount) {
        this.rewarmCount = rewarmCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DnsCacheEntry that = (DnsCacheEntry) o;
        return expiryTime == that.expiryTime && configuredTtlInSeconds == that.configuredTtlInSeconds && rewarmCount == that.rewarmCount && Objects.equals(response, that.response);
    }

    @Override
    public int hashCode() {
        return Objects.hash(response, expiryTime, configuredTtlInSeconds, rewarmCount);
    }
}