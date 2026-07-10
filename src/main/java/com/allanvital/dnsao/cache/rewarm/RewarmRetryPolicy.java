package com.allanvital.dnsao.cache.rewarm;

public class RewarmRetryPolicy {

    private final int maxTransientRetries;

    public RewarmRetryPolicy(int maxTransientRetries) {
        this.maxTransientRetries = Math.max(0, maxTransientRetries);
    }

    public boolean shouldRetry(int attempt, RewarmAttemptResult result) {
        return result != null && result.retryable() && attempt < maxTransientRetries;
    }
}
