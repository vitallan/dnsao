package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final AuthorityQueryClient authorityQueryClient;
    private final ReferralInterpreter referralInterpreter;

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient, ReferralInterpreter referralInterpreter) {
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
    }

    public RecursiveSession buildRecursiveSession(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        return new RecursiveSession(dnsQueryRequest, rootHints, authorityQueryClient, referralInterpreter);
    }
}
