package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryOutcome;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.DelegationPoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.ReferralResult;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSession {

    protected final DnsQueryRequest dnsQueryRequest;
    protected final List<AuthorityEndpoint> rootHints;
    protected final AuthorityQueryClient authorityQueryClient;
    protected final ReferralInterpreter referralInterpreter;
    protected final MinimizedQuestionProvider minimizedQuestionProvider;

    public RecursiveSession(DnsQueryRequest dnsQueryRequest,
                            List<AuthorityEndpoint> rootHints,
                            AuthorityQueryClient authorityQueryClient,
                            ReferralInterpreter referralInterpreter,
                            MinimizedQuestionProvider minimizedQuestionProvider) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.rootHints = List.copyOf(rootHints);
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
        this.minimizedQuestionProvider = minimizedQuestionProvider;
    }

    public RecursiveResult resolve() {
        Message originalQuery = dnsQueryRequest.getRequest();
        if (originalQuery == null || originalQuery.getQuestion() == null || rootHints.isEmpty()) {
            return servfailResult(originalQuery, "missing_query_or_root_hints");
        }

        List<Message> authorityDiscoveryQuestions = minimizedQuestionProvider.buildAuthorityDiscoveryQuestions(originalQuery);
        AuthorityEndpoint currentAuthority = rootHints.get(0);
        AuthorityEndpoint finalAuthority = null;

        for (Message authorityDiscoveryQuestion : authorityDiscoveryQuestions) {
            ReferralResult referralResult = executeAndInterpret(currentAuthority, authorityDiscoveryQuestion);
            if (referralResult == null || referralResult.getType() != ReferralResult.Type.REFERRAL) {
                return servfailResult(originalQuery, "invalid_referral_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            finalAuthority = getFirstAuthority(referralResult.getDelegationPoint());
            if (finalAuthority == null) {
                return servfailResult(originalQuery, "missing_authority_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            currentAuthority = finalAuthority;
        }

        if (finalAuthority == null) {
            return servfailResult(originalQuery, "missing_final_authority");
        }

        ReferralResult finalAnswer = executeAndInterpret(finalAuthority, originalQuery);
        if (finalAnswer != null && finalAnswer.getType() == ReferralResult.Type.FINAL_ANSWER && finalAnswer.getFinalAnswer() != null) {
            return RecursiveResult.answer(finalAnswer.getFinalAnswer());
        }

        return servfailResult(originalQuery, "invalid_final_answer");
    }

    private ReferralResult executeAndInterpret(AuthorityEndpoint authorityEndpoint, Message query) {
        AuthorityQueryResult queryResult = authorityQueryClient.query(authorityEndpoint, query);
        if (queryResult == null || queryResult.getOutcome() != AuthorityQueryOutcome.SUCCESS || queryResult.getResponse() == null) {
            return null;
        }
        return referralInterpreter.interpret(queryResult.getResponse());
    }

    private AuthorityEndpoint getFirstAuthority(DelegationPoint delegationPoint) {
        if (delegationPoint == null || delegationPoint.authorityEndpoints() == null || delegationPoint.authorityEndpoints().isEmpty()) {
            return null;
        }
        return delegationPoint.authorityEndpoints().get(0);
    }

    private RecursiveResult servfailResult(Message query, String note) {
        return RecursiveResult.servfail(buildServfail(query), note);
    }

    private Message buildServfail(Message query) {
        if (query == null) {
            Message fail = new Message();
            fail.getHeader().setFlag(Flags.QR);
            fail.getHeader().setRcode(Rcode.SERVFAIL);
            return fail;
        }
        Message fail = new Message(query.getHeader().getID());
        fail.getHeader().setFlag(Flags.QR);
        fail.addRecord(query.getQuestion(), Section.QUESTION);
        fail.getHeader().setRcode(Rcode.SERVFAIL);
        return fail;
    }
}
