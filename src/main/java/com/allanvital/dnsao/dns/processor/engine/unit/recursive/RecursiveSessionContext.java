package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSessionContext {

    private final DnsQueryRequest dnsQueryRequest;
    private final List<AuthorityEndpoint> rootHints;
    private final RecursiveExecutionBudget recursiveExecutionBudget;
    private final int subqueryDepth;
    private final long startTimeMillis;
    private final long deadlineTimeMillis;
    private final int perAuthorityTimeoutMillis;

    public RecursiveSessionContext(DnsQueryRequest dnsQueryRequest,
                                   List<AuthorityEndpoint> rootHints,
                                   RecursiveExecutionBudget recursiveExecutionBudget,
                                   int subqueryDepth,
                                   long startTimeMillis,
                                   long deadlineTimeMillis,
                                   int perAuthorityTimeoutMillis) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.rootHints = List.copyOf(rootHints);
        this.recursiveExecutionBudget = recursiveExecutionBudget;
        this.subqueryDepth = subqueryDepth;
        this.startTimeMillis = startTimeMillis;
        this.deadlineTimeMillis = deadlineTimeMillis;
        this.perAuthorityTimeoutMillis = perAuthorityTimeoutMillis;
    }

    public DnsQueryRequest getDnsQueryRequest() {
        return dnsQueryRequest;
    }

    public List<AuthorityEndpoint> getRootHints() {
        return rootHints;
    }

    public RecursiveExecutionBudget getRecursiveExecutionBudget() {
        return recursiveExecutionBudget;
    }

    public int getSubqueryDepth() {
        return subqueryDepth;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getDeadlineTimeMillis() {
        return deadlineTimeMillis;
    }

    public int getPerAuthorityTimeoutMillis() {
        return perAuthorityTimeoutMillis;
    }
}
