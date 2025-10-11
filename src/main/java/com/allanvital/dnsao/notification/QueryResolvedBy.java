package com.allanvital.dnsao.notification;

public enum QueryResolvedBy {

    CACHE,
    UPSTREAM,
    BLOCKED,
    REFUSED,
    SERVFAIL,
    LOCAL

}
