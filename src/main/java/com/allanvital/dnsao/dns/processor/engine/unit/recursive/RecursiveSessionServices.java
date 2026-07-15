package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.*;
import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionServices {

    private final AuthorityQueryClient authorityQueryClient;
    private final ReferralInterpreter referralInterpreter;
    private final MinimizedQuestionProvider minimizedQuestionProvider;
    private final RecursiveSessionFactory recursiveSessionFactory;

    public RecursiveSessionServices(AuthorityQueryClient authorityQueryClient,
                                    ReferralInterpreter referralInterpreter,
                                    MinimizedQuestionProvider minimizedQuestionProvider,
                                    RecursiveSessionFactory recursiveSessionFactory) {
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
        this.minimizedQuestionProvider = minimizedQuestionProvider;
        this.recursiveSessionFactory = recursiveSessionFactory;
    }

    public List<Message> buildAuthorityDiscoveryQuestions(Message originalQuery) {
        return minimizedQuestionProvider.buildAuthorityDiscoveryQuestions(originalQuery);
    }

    public ReferralResult queryAuthority(RecursiveSessionContext recursiveSessionContext, AuthorityEndpoint authorityEndpoint, Message query) {
        if (recursiveSessionContext == null || recursiveSessionContext.getRecursiveExecutionBudget() == null) {
            return null;
        }
        if (isDeadlineExceeded(recursiveSessionContext)) {
            return null;
        }
        if (!recursiveSessionContext.getRecursiveExecutionBudget().tryConsumeStep()) {
            return null;
        }
        long beforeQuery = Clock.currentTimeInMillis();
        AuthorityQueryResult queryResult = authorityQueryClient.query(authorityEndpoint, query);
        long afterQuery = Clock.currentTimeInMillis();
        if ((afterQuery - beforeQuery) > recursiveSessionContext.getPerAuthorityTimeoutMillis()) {
            return null;
        }
        if (isDeadlineExceeded(recursiveSessionContext)) {
            return null;
        }
        if (queryResult == null || queryResult.getOutcome() != AuthorityQueryOutcome.SUCCESS || queryResult.getResponse() == null) {
            return null;
        }
        return referralInterpreter.interpret(queryResult.getResponse());
    }

    public AuthorityEndpoint resolveNameserverAddress(String nameserverName, RecursiveSessionContext recursiveSessionContext) {
        RecursiveResult recursiveResult = recursiveSessionFactory.resolveSubquery(Type.A, nameserverName, recursiveSessionContext);
        if (recursiveResult == null || recursiveResult.getFinalMessage() == null) {
            return null;
        }
        String ip = DnsUtils.extractIpFromResponseMessage(recursiveResult.getFinalMessage());
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            return new AuthorityEndpoint(nameserverName, InetAddress.getByName(ip), 53);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDeadlineExceeded(RecursiveSessionContext recursiveSessionContext) {
        return Clock.currentTimeInMillis() > recursiveSessionContext.getDeadlineTimeMillis();
    }
}
