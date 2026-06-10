package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Message;

public class RecursiveSessionFactory {

    private final int timeoutMs;

    public RecursiveSessionFactory(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public RecursiveSession createSession(Message request) {
        return new RecursiveSession(request, timeoutMs);
    }

}
