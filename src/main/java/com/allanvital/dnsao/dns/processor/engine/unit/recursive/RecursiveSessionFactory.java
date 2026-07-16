package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.conf.inner.RecursiveConf;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import com.allanvital.dnsao.infra.clock.Clock;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionFactory {

    private final AuthorityQueryClient authorityQueryClient;
    private final ReferralInterpreter referralInterpreter;
    private final MinimizedQuestionProvider minimizedQuestionProvider;
    private final RecursiveInternalRequestFactory recursiveInternalRequestFactory;
    private final RecursiveConf recursiveConf;

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient, ReferralInterpreter referralInterpreter) {
        this(authorityQueryClient, referralInterpreter, new MinimizedQuestionProvider(), new RecursiveInternalRequestFactory(), new RecursiveConf());
    }

    public RecursiveSessionFactory(AuthorityQueryClient authorityQueryClient,
                                   ReferralInterpreter referralInterpreter,
                                   MinimizedQuestionProvider minimizedQuestionProvider,
                                   RecursiveInternalRequestFactory recursiveInternalRequestFactory,
                                   RecursiveConf recursiveConf) {
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
        this.minimizedQuestionProvider = minimizedQuestionProvider;
        this.recursiveInternalRequestFactory = recursiveInternalRequestFactory;
        this.recursiveConf = recursiveConf;
    }

    public RecursiveSession buildRecursiveSession(DnsQueryRequest dnsQueryRequest, List<AuthorityEndpoint> rootHints) {
        RecursiveExecutionBudget recursiveExecutionBudget = new RecursiveExecutionBudget(recursiveConf.getMaxSteps());
        long startTimeMillis = Clock.currentTimeInMillis();
        long deadlineTimeMillis = startTimeMillis + (recursiveConf.getTimeoutSeconds() * 1000L);
        RecursiveSessionContext recursiveSessionContext = new RecursiveSessionContext(
                dnsQueryRequest,
                rootHints,
                recursiveExecutionBudget,
                0,
                startTimeMillis,
                deadlineTimeMillis,
                recursiveConf.getPerAuthorityTimeoutMillis()
        );
        RecursiveSessionServices recursiveSessionServices = new RecursiveSessionServices(
                authorityQueryClient,
                referralInterpreter,
                minimizedQuestionProvider,
                this,
                recursiveConf.getMaxRetries(),
                recursiveConf.getMaxCnameRedirects()
        );
        return new RecursiveSession(recursiveSessionContext, recursiveSessionServices);
    }

    public RecursiveResult resolveSubquery(int type, String qname, RecursiveSessionContext parentContext) {
        if (parentContext == null) {
            return null;
        }
        int childDepth = parentContext.getSubqueryDepth() + 1;
        DnsQueryRequest dnsQueryRequest = recursiveInternalRequestFactory.buildInternalQueryRequest(type, qname);
        if (childDepth > recursiveConf.getMaxSubqueryDepth()) {
            return RecursiveResult.servfail(buildServfail(dnsQueryRequest), "recursive_subquery_depth_exceeded");
        }
        RecursiveSessionContext recursiveSessionContext = new RecursiveSessionContext(
                dnsQueryRequest,
                parentContext.getRootHints(),
                parentContext.getRecursiveExecutionBudget(),
                childDepth,
                parentContext.getStartTimeMillis(),
                parentContext.getDeadlineTimeMillis(),
                parentContext.getPerAuthorityTimeoutMillis()
        );
        RecursiveSessionServices recursiveSessionServices = new RecursiveSessionServices(
                authorityQueryClient,
                referralInterpreter,
                minimizedQuestionProvider,
                this,
                recursiveConf.getMaxRetries(),
                recursiveConf.getMaxCnameRedirects()
        );
        return new RecursiveSession(recursiveSessionContext, recursiveSessionServices).resolve();
    }

    private org.xbill.DNS.Message buildServfail(DnsQueryRequest dnsQueryRequest) {
        org.xbill.DNS.Message query = dnsQueryRequest != null ? dnsQueryRequest.getRequest() : null;
        if (query == null) {
            org.xbill.DNS.Message fail = new org.xbill.DNS.Message();
            fail.getHeader().setFlag(org.xbill.DNS.Flags.QR);
            fail.getHeader().setRcode(org.xbill.DNS.Rcode.SERVFAIL);
            return fail;
        }
        org.xbill.DNS.Message fail = new org.xbill.DNS.Message(query.getHeader().getID());
        fail.getHeader().setFlag(org.xbill.DNS.Flags.QR);
        fail.addRecord(query.getQuestion(), org.xbill.DNS.Section.QUESTION);
        fail.getHeader().setRcode(org.xbill.DNS.Rcode.SERVFAIL);
        return fail;
    }

}
