package com.allanvital.dnsao.infra.notification;

public enum QueryResolvedBy {

    CACHE,
    UPSTREAM,
    BLOCKED,
    REFUSED,
    SERVFAIL,
    LOCAL

}
