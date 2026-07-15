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
        AuthorityEndpoint currentAuthority = recursiveSessionContext.getRootHints().get(0);
        AuthorityEndpoint finalAuthority = null;

        for (Message authorityDiscoveryQuestion : authorityDiscoveryQuestions) {
            ReferralResult referralResult = recursiveSessionServices.queryAuthority(recursiveSessionContext, currentAuthority, authorityDiscoveryQuestion);
            if (referralResult == null || referralResult.getType() != ReferralResult.Type.REFERRAL) {
                return servfailResult(originalQuery, "invalid_referral_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            finalAuthority = resolveAuthority(referralResult.getDelegationPoint());
            if (finalAuthority == null) {
                return servfailResult(originalQuery, "missing_authority_for_" + authorityDiscoveryQuestion.getQuestion().getName());
            }
            currentAuthority = finalAuthority;
        }

        if (finalAuthority == null) {
            return servfailResult(originalQuery, "missing_final_authority");
        }

        ReferralResult finalAnswer = recursiveSessionServices.queryAuthority(recursiveSessionContext, finalAuthority, originalQuery);
        if (finalAnswer != null && finalAnswer.getType() == ReferralResult.Type.FINAL_ANSWER && finalAnswer.getFinalAnswer() != null) {
            return RecursiveResult.answer(finalAnswer.getFinalAnswer());
        }

        return servfailResult(originalQuery, "invalid_final_answer");
    }

    private AuthorityEndpoint getFirstAuthority(DelegationPoint delegationPoint) {
        if (delegationPoint == null || delegationPoint.authorityEndpoints() == null || delegationPoint.authorityEndpoints().isEmpty()) {
            return null;
        }
        return delegationPoint.authorityEndpoints().get(0);
    }

    private AuthorityEndpoint resolveAuthority(DelegationPoint delegationPoint) {
        AuthorityEndpoint authorityFromGlue = getFirstAuthority(delegationPoint);
        if (authorityFromGlue != null) {
            return authorityFromGlue;
        }
        if (delegationPoint == null || delegationPoint.nameserverNames() == null || delegationPoint.nameserverNames().isEmpty()) {
            return null;
        }
        String nameserverName = delegationPoint.nameserverNames().get(0);
        return recursiveSessionServices.resolveNameserverAddress(nameserverName, recursiveSessionContext);
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
