package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
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

    protected final RecursiveSessionContext recursiveSessionContext;
    protected final RecursiveSessionServices recursiveSessionServices;

    public RecursiveSession(RecursiveSessionContext recursiveSessionContext,
                            RecursiveSessionServices recursiveSessionServices) {
        this.recursiveSessionContext = recursiveSessionContext;
        this.recursiveSessionServices = recursiveSessionServices;
    }

    public RecursiveResult resolve() {
        Message originalQuery = recursiveSessionContext.getDnsQueryRequest().getRequest();
        if (originalQuery == null || originalQuery.getQuestion() == null || recursiveSessionContext.getRootHints().isEmpty()) {
            return servfailResult(originalQuery, "missing_query_or_root_hints");
        }

        List<Message> authorityDiscoveryQuestions = recursiveSessionServices.buildAuthorityDiscoveryQuestions(originalQuery);
        List<AuthorityEndpoint> currentAuthorities = recursiveSessionContext.getRootHints();
        List<AuthorityEndpoint> finalAuthorities = null;

        for (Message authorityDiscoveryQuestion : authorityDiscoveryQuestions) {
            ReferralResult referralResult = queryAuthoritiesForReferral(currentAuthorities, authorityDiscoveryQuestion);
            if (referralResult == null || referralResult.getType() != ReferralResult.Type.REFERRAL) {
                return servfailResult(originalQuery, "invalid_referral_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            finalAuthorities = resolveAuthorities(referralResult.getDelegationPoint());
            if (finalAuthorities == null || finalAuthorities.isEmpty()) {
                return servfailResult(originalQuery, "missing_authority_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            currentAuthorities = finalAuthorities;
        }

        if (finalAuthorities == null || finalAuthorities.isEmpty()) {
            return servfailResult(originalQuery, "missing_final_authority");
        }

        ReferralResult finalAnswer = queryAuthoritiesForFinalAnswer(finalAuthorities, originalQuery);
        if (finalAnswer != null && finalAnswer.getType() == ReferralResult.Type.FINAL_ANSWER && finalAnswer.getFinalAnswer() != null) {
            return RecursiveResult.answer(finalAnswer.getFinalAnswer());
        }

        return servfailResult(originalQuery, "invalid_final_answer");
    }

    private ReferralResult queryAuthoritiesForReferral(List<AuthorityEndpoint> authorityEndpoints, Message query) {
        if (authorityEndpoints == null || authorityEndpoints.isEmpty()) {
            return null;
        }
        for (AuthorityEndpoint authorityEndpoint : authorityEndpoints) {
            ReferralResult referralResult = recursiveSessionServices.queryAuthority(recursiveSessionContext, authorityEndpoint, query);
            if (referralResult != null && referralResult.getType() == ReferralResult.Type.REFERRAL) {
                return referralResult;
            }
        }
        return null;
    }

    private ReferralResult queryAuthoritiesForFinalAnswer(List<AuthorityEndpoint> authorityEndpoints, Message query) {
        if (authorityEndpoints == null || authorityEndpoints.isEmpty()) {
            return null;
        }
        for (AuthorityEndpoint authorityEndpoint : authorityEndpoints) {
            ReferralResult referralResult = recursiveSessionServices.queryAuthority(recursiveSessionContext, authorityEndpoint, query);
            if (referralResult != null && referralResult.getType() == ReferralResult.Type.FINAL_ANSWER) {
                return referralResult;
            }
        }
        return null;
    }

    private List<AuthorityEndpoint> resolveAuthorities(DelegationPoint delegationPoint) {
        List<AuthorityEndpoint> authorityFromGlue = delegationPoint != null ? delegationPoint.authorityEndpoints() : null;
        if (authorityFromGlue != null && !authorityFromGlue.isEmpty()) {
            return authorityFromGlue;
        }
        if (delegationPoint == null || delegationPoint.nameserverNames() == null || delegationPoint.nameserverNames().isEmpty()) {
            return null;
        }
        String nameserverName = delegationPoint.nameserverNames().get(0);
        AuthorityEndpoint authorityEndpoint = recursiveSessionServices.resolveNameserverAddress(nameserverName, recursiveSessionContext);
        if (authorityEndpoint == null) {
            return null;
        }
        return List.of(authorityEndpoint);
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
