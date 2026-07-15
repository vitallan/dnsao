package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.AuthorityQueryClient;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import org.xbill.DNS.Message;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FailSpecificAuthorityQueryClient implements AuthorityQueryClient {

    private final AuthorityQueryClient delegate;
    private final AuthorityEndpoint authorityToFail;
    private final List<AuthorityQueryCall> calls = new ArrayList<>();

    public FailSpecificAuthorityQueryClient(AuthorityQueryClient delegate, AuthorityEndpoint authorityToFail) {
        this.delegate = delegate;
        this.authorityToFail = authorityToFail;
    }

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        calls.add(new AuthorityQueryCall(authorityEndpoint, query));
        if (authorityToFail.equals(authorityEndpoint)) {
            return AuthorityQueryResult.timeout(authorityEndpoint, new RuntimeException("forced_authority_timeout"));
        }
        return delegate.query(authorityEndpoint, query);
    }

    public void assertCallCount(int expected) {
        assertEquals(expected, calls.size());
    }

    public void assertAuthorityCalledAt(int index, AuthorityEndpoint authorityEndpoint, int type, String qname) {
        if (index < 0 || index >= calls.size()) {
            fail("no authority call at index=" + index + ", total calls=" + calls.size());
        }
        AuthorityQueryCall call = calls.get(index);
        assertEquals(authorityEndpoint, call.authorityEndpoint());
        QuestionDefinition expectedQuestion = new QuestionDefinition(type, qname);
        if (!expectedQuestion.matches(call.query() != null ? call.query().getQuestion() : null)) {
            fail("unexpected question at index=" + index + ", expected type=" + type + " qname=" + qname);
        }
    }
}
