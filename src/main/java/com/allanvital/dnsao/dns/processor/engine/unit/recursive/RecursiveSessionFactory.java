package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final AuthorityQueryClient authorityQueryClient;
    private final ReferralInterpreter referralInterpreter;
    private final MinimizedQuestionProvider minimizedQuestionProvider;
    private final RecursiveInternalRequestFactory recursiveInternalRequestFactory;

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient, ReferralInterpreter referralInterpreter) {
        this(authorityQueryClient, referralInterpreter, new MinimizedQuestionProvider(), new RecursiveInternalRequestFactory());
    }

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient,
                                   ReferralInterpreter referralInterpreter,
                                   MinimizedQuestionProvider minimizedQuestionProvider,
                                   RecursiveInternalRequestFactory recursiveInternalRequestFactory) {
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
        this.minimizedQuestionProvider = minimizedQuestionProvider;
        this.recursiveInternalRequestFactory = recursiveInternalRequestFactory;
    }

    public RecursiveSession buildRecursiveSession(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        RecursiveSessionContext recursiveSessionContext = new RecursiveSessionContext(dnsQueryRequest, rootHints);
        RecursiveSessionServices recursiveSessionServices = new RecursiveSessionServices(
                authorityQueryClient,
                referralInterpreter,
                minimizedQuestionProvider,
                this
        );
        return new RecursiveSession(recursiveSessionContext, recursiveSessionServices);
    }

    public RecursiveResult resolveSubquery(int type, String qname, List<AuthorityEndpoint> rootHints) {
        DnsQueryRequest dnsQueryRequest = recursiveInternalRequestFactory.buildInternalQueryRequest(type, qname);
        return buildRecursiveSession(dnsQueryRequest, rootHints).resolve();
    }

}
