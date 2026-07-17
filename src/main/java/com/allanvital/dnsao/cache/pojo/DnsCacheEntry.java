package com.allanvital.dnsao.cache.pojo;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Message;

import java.util.Objects;

import static com.allanvital.dnsao.utils.TimeUtils.formatMillisTime;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsCacheEntry {


    private final Message response;
    private final long expiryTime;
    private final long configuredTtlInSeconds;
    private int rewarmCount = 0;
    private long lastAccessSeq = 0;
    private int transientRewarmFailureCount = 0;

    public DnsCacheEntry(Message response, Long ttlInSeconds) {
        this.response = response;
        if (ttlInSeconds == null) {
            this.configuredTtlInSeconds = 30;
        } else {
            this.configuredTtlInSeconds = ttlInSeconds;
        }
        long ttlMs = Math.multiplyExact(configuredTtlInSeconds, 1000L);
        long currentTimeInMillis = getCurrentTimeInMillis();
        this.expiryTime = Math.addExact(currentTimeInMillis, ttlMs);
        Log.CACHE.trace("new cacheEntry ttlInSecs={} : ttlInMs={} : currentTime={} : expiryTime={} ",
                ttlInSeconds,
                ttlMs,
                formatMillisTime(currentTimeInMillis),
                formatMillisTime(this.expiryTime));
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

    public long getConfiguredTtlInSeconds() {
        return configuredTtlInSeconds;
    }

    public boolean isStale() {
        return getCurrentTimeInMillis() > expiryTime;
    }

    public boolean isExpired(int maxServeExpiredInSeconds) {
        return getCurrentTimeInMillis() > expiryTime + (maxServeExpiredInSeconds * 1000L);
    }

    private long getCurrentTimeInMillis() {
        return Clock.currentTimeInMillis();
    }

    public int getRewarmCount() {
        return rewarmCount;
    }

    public void setRewarmCount(int rewarmCount) {
        this.rewarmCount = rewarmCount;
    }

    public long getLastAccessSeq() {
        return lastAccessSeq;
    }

    public void setLastAccessSeq(long lastAccessSeq) {
        this.lastAccessSeq = lastAccessSeq;
    }

    public int getTransientRewarmFailureCount() {
        return transientRewarmFailureCount;
    }

    public void setTransientRewarmFailureCount(int transientRewarmFailureCount) {
        this.transientRewarmFailureCount = transientRewarmFailureCount;
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
