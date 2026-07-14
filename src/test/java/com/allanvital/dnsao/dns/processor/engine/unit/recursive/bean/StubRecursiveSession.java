package com.allanvital.dnsao.dns.processor.engine.unit.recursive.bean;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.RecursiveSession;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StubRecursiveSession extends RecursiveSession {

    private final RecursiveResult recursiveResult;

    public StubRecursiveSession(DnsQueryRequest dnsQueryRequest, RecursiveResult recursiveResult) {
        super(dnsQueryRequest, List.of(), null, null);
        this.recursiveResult = recursiveResult;
    }

    @Override
    public RecursiveResult resolve() {
        return recursiveResult;
    }
}
