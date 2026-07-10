package com.allanvital.dnsao.cache.rewarm;

public enum RewarmSkipReason {
    MISSING_ENTRY("missing_entry"),
    OBSOLETE_TASK("obsolete_task"),
    MISSING_QUESTION("missing_question"),
    NOT_WARMABLE_CACHED_RESPONSE("not_warmable_cached_response"),
    MAX_REWARM_REACHED("max_rewarm_reached"),
    REMOVED_BEFORE_STORE("removed_before_store"),
    REPLACED_BEFORE_STORE("replaced_before_store");

    public static final String MESSAGE = "rewarm skip: key={} reason={} expectedExpiry={} currentExpiry={}";

    private final String reason;

    RewarmSkipReason(String reason) {
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
