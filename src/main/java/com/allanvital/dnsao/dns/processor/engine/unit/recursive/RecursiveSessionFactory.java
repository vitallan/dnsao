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
    private final MinimizedQuestionProvider minimizedQuestionProvider;

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient, ReferralInterpreter referralInterpreter) {
        this(authorityQueryClient, referralInterpreter, new MinimizedQuestionProvider());
    }

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient,
                                   ReferralInterpreter referralInterpreter,
                                   MinimizedQuestionProvider minimizedQuestionProvider) {
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
        this.minimizedQuestionProvider = minimizedQuestionProvider;
    }

    public RecursiveSession buildRecursiveSession(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        return new RecursiveSession(dnsQueryRequest, rootHints, authorityQueryClient, referralInterpreter, minimizedQuestionProvider);
    }
}
