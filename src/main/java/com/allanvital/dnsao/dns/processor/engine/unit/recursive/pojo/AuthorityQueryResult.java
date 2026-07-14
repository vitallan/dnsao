package com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo;

import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class AuthorityQueryResult {

    private final AuthorityEndpoint authorityEndpoint;
    private final AuthorityQueryOutcome outcome;
    private final Message response;
    private final Throwable error;

    private AuthorityQueryResult(AuthorityEndpoint authorityEndpoint,
                                 AuthorityQueryOutcome outcome,
                                 Message response,
                                 Throwable error) {
        this.authorityEndpoint = authorityEndpoint;
        this.outcome = outcome;
        this.response = response;
        this.error = error;
    }

    public static AuthorityQueryResult success(AuthorityEndpoint authorityEndpoint, Message response) {
        return new AuthorityQueryResult(authorityEndpoint, AuthorityQueryOutcome.SUCCESS, response, null);
    }

    public static AuthorityQueryResult timeout(AuthorityEndpoint authorityEndpoint, Throwable error) {
        return new AuthorityQueryResult(authorityEndpoint, AuthorityQueryOutcome.TIMEOUT, null, error);
    }

    public static AuthorityQueryResult error(AuthorityEndpoint authorityEndpoint, Throwable error) {
        return new AuthorityQueryResult(authorityEndpoint, AuthorityQueryOutcome.ERROR, null, error);
    }

    public static AuthorityQueryResult truncated(AuthorityEndpoint authorityEndpoint, Message response) {
        return new AuthorityQueryResult(authorityEndpoint, AuthorityQueryOutcome.TRUNCATED, response, null);
    }

    public AuthorityEndpoint getAuthorityEndpoint() {
        return authorityEndpoint;
    }

    public AuthorityQueryOutcome getOutcome() {
        return outcome;
    }

    public Message getResponse() {
        return response;
    }

    public Throwable getError() {
        return error;
    }
}
