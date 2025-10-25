package com.allanvital.dnsao.cache.rewarm;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmTask implements Delayed {

    private final String key;
    private final long triggerAtMs;

    public RewarmTask(String key, long triggerAtMs) {
        this.key = key;
        this.triggerAtMs = triggerAtMs;
    }

    public String getKey() {
        return key;
    }

    public long getTriggerAtMs() {
        return triggerAtMs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RewarmTask that = (RewarmTask) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        long delayMs = triggerAtMs - System.currentTimeMillis();
        return unit.convert(delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        if (this == o) return 0;
        long other = ((RewarmTask) o).triggerAtMs;
        return Long.compare(this.triggerAtMs, other);
    }
}