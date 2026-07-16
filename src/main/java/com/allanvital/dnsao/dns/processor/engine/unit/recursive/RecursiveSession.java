package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.DelegationPoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.ReferralResult;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import org.xbill.DNS.*;

import java.util.ArrayList;
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

        Message currentQuery = originalQuery;
        List<org.xbill.DNS.Record> cnameChain = new ArrayList<>();
        int cnameRedirects = 0;

        while (true) {
            ResolutionAttempt resolutionAttempt = resolveCurrentQuery(currentQuery);
            if (resolutionAttempt == null || resolutionAttempt.response() == null) {
                return servfailResult(originalQuery, "invalid_final_answer");
            }

            Message response = resolutionAttempt.response();
            if (hasDirectAnswer(response, currentQuery.getQuestion().getType())) {
                return RecursiveResult.answer(buildClientResponse(originalQuery, cnameChain, response));
            }

            CNAMERecord cnameRecord = extractFirstCname(response);
            if (cnameRecord == null) {
                return servfailResult(originalQuery, "invalid_final_answer");
            }
            if (cnameRedirects >= recursiveSessionServices.getMaxCnameRedirects()) {
                return servfailResult(originalQuery, "cname_redirect_limit_exceeded");
            }

            cnameChain.add(cnameRecord);
            currentQuery = recursiveSessionServices.buildTargetQuestion(originalQuery, cnameRecord.getTarget().toString());
            cnameRedirects++;
        }
    }

    private ResolutionAttempt resolveCurrentQuery(Message currentQuery) {
        List<Message> authorityDiscoveryQuestions = recursiveSessionServices.buildAuthorityDiscoveryQuestions(currentQuery);
        List<AuthorityEndpoint> currentAuthorities = recursiveSessionContext.getRootHints();
        List<AuthorityEndpoint> finalAuthorities = null;

        for (Message authorityDiscoveryQuestion : authorityDiscoveryQuestions) {
            ReferralResult referralResult = queryAuthoritiesForReferral(currentAuthorities, authorityDiscoveryQuestion);
            if (referralResult == null || referralResult.getType() != ReferralResult.Type.REFERRAL) {
                if (isSameQuestionName(authorityDiscoveryQuestion, currentQuery)) {
                    finalAuthorities = currentAuthorities;
                    break;
                }
                return null;
            }
            finalAuthorities = resolveAuthorities(referralResult.getDelegationPoint());
            if (finalAuthorities == null || finalAuthorities.isEmpty()) {
                return null;
            }
            currentAuthorities = finalAuthorities;
        }

        if (finalAuthorities == null || finalAuthorities.isEmpty()) {
            return null;
        }

        ReferralResult finalAnswer = queryAuthoritiesForFinalAnswer(finalAuthorities, currentQuery);
        if (finalAnswer != null && finalAnswer.getType() == ReferralResult.Type.FINAL_ANSWER && finalAnswer.getFinalAnswer() != null) {
            return new ResolutionAttempt(finalAnswer.getFinalAnswer());
        }
        return null;
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

    private boolean hasDirectAnswer(Message response, int type) {
        List<org.xbill.DNS.Record> answers = response.getSection(Section.ANSWER);
        for (org.xbill.DNS.Record answer : answers) {
            if (answer.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private CNAMERecord extractFirstCname(Message response) {
        List<org.xbill.DNS.Record> answers = response.getSection(Section.ANSWER);
        for (org.xbill.DNS.Record answer : answers) {
            if (answer instanceof CNAMERecord cnameRecord) {
                return cnameRecord;
            }
        }
        return null;
    }

    private Message buildClientResponse(Message originalQuery, List<org.xbill.DNS.Record> cnameChain, Message terminalResponse) {
        Message response = DnsUtils.baseResponse(originalQuery, originalQuery.getQuestion());
        response.getHeader().setRcode(terminalResponse.getRcode());
        for (org.xbill.DNS.Record cnameRecord : cnameChain) {
            response.addRecord(cnameRecord, Section.ANSWER);
        }
        for (org.xbill.DNS.Record answer : terminalResponse.getSection(Section.ANSWER)) {
            response.addRecord(answer, Section.ANSWER);
        }
        return response;
    }

    private boolean isSameQuestionName(Message authorityDiscoveryQuestion, Message currentQuery) {
        return authorityDiscoveryQuestion.getQuestion().getName().equals(currentQuery.getQuestion().getName());
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

    private record ResolutionAttempt(Message response) {
    }
}
