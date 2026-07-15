package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryOutcome;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
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

    public com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.ReferralResult queryAuthority(AuthorityEndpoint authorityEndpoint, Message query) {
        AuthorityQueryResult queryResult = authorityQueryClient.query(authorityEndpoint, query);
        if (queryResult == null || queryResult.getOutcome() != AuthorityQueryOutcome.SUCCESS || queryResult.getResponse() == null) {
            return null;
        }
        return referralInterpreter.interpret(queryResult.getResponse());
    }

    public AuthorityEndpoint resolveNameserverAddress(String nameserverName, List<AuthorityEndpoint> rootHints) {
        RecursiveResult recursiveResult = recursiveSessionFactory.resolveSubquery(Type.A, nameserverName, rootHints);
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
}
