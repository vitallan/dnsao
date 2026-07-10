package com.allanvital.dnsao.cache.rewarm;

import org.xbill.DNS.Message;

public record RewarmAttemptResult(boolean success, Message response, boolean retryable, String failureReason) {

    public static RewarmAttemptResult success(Message response) {
        return new RewarmAttemptResult(true, response, false, null);
    }

    public static RewarmAttemptResult retryableFailure(String reason) {
        return new RewarmAttemptResult(false, null, true, reason);
    }

    public static RewarmAttemptResult terminalFailure(String reason) {
        return new RewarmAttemptResult(false, null, false, reason);
    }
}
