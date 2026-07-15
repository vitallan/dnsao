package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.AuthorityQueryClient;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import org.xbill.DNS.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FailFirstAuthorityQueryClient implements AuthorityQueryClient {

    private final AuthorityQueryClient delegate;
    private boolean firstCall = true;
    private final java.util.List<AuthorityQueryCall> calls = new java.util.ArrayList<>();

    public FailFirstAuthorityQueryClient(AuthorityQueryClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        calls.add(new AuthorityQueryCall(authorityEndpoint, query));
        if (firstCall) {
            firstCall = false;
            return AuthorityQueryResult.timeout(authorityEndpoint, new RuntimeException("forced_first_timeout"));
        }
        return delegate.query(authorityEndpoint, query);
    }

    public void assertCallCount(int expected) {
        assertEquals(expected, calls.size());
    }

    public void assertAuthorityCalledTimes(AuthorityEndpoint authorityEndpoint, int expectedCount, int type, String qname) {
        QuestionDefinition expectedQuestion = new QuestionDefinition(type, qname);
        int actual = 0;
        for (AuthorityQueryCall call : calls) {
            if (!authorityEndpoint.equals(call.authorityEndpoint())) {
                continue;
            }
            if (expectedQuestion.matches(call.query() != null ? call.query().getQuestion() : null)) {
                actual++;
            }
        }
        assertEquals(expectedCount, actual);
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
