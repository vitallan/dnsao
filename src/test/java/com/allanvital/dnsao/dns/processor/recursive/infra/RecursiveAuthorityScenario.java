package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.AuthorityQueryClient;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthorityScenario implements AuthorityQueryClient {

    private final List<RecursiveAuthorityScenarioEntry> entries = new ArrayList<>();
    private final List<AuthorityQueryCall> calls = new ArrayList<>();
    private final RecursiveAuthorityMessageFactory messageFactory = new RecursiveAuthorityMessageFactory();

    public RecursiveAuthorityScenarioWhenAsked whenAsked(AuthorityEndpoint authorityEndpoint) {
        return new RecursiveAuthorityScenarioWhenAsked(this, authorityEndpoint);
    }

    public void addEntry(RecursiveAuthorityScenarioEntry entry) {
        entries.add(entry);
    }

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        calls.add(new AuthorityQueryCall(authorityEndpoint, query));
        RecursiveAuthorityScenarioEntry entry = findEntry(authorityEndpoint, query != null ? query.getQuestion() : null);
        if (entry == null) {
            fail("no scenario entry for authority=" + authorityEndpoint + " question=" + (query != null ? query.getQuestion() : null));
        }
        if (entry.getReferralAnswer() != null) {
            return AuthorityQueryResult.success(authorityEndpoint, messageFactory.buildReferral(query, entry.getReferralAnswer()));
        }
        if (entry.getPositiveAnswer() != null) {
            return AuthorityQueryResult.success(authorityEndpoint, messageFactory.buildPositiveAnswer(query, entry.getPositiveAnswer()));
        }
        fail("scenario entry must define a response for authority=" + authorityEndpoint);
        return null;
    }

    public List<AuthorityQueryCall> getCalls() {
        return calls;
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
        Record question = call.query() != null ? call.query().getQuestion() : null;
        if (!expectedQuestion.matches(question)) {
            fail("unexpected question at index=" + index + ", expected type=" + type + " qname=" + qname + " but got=" + question);
        }
    }

    public void assertAuthorityCalledTimes(AuthorityEndpoint authorityEndpoint, int expectedCount, int type, String qname) {
        QuestionDefinition expectedQuestion = new QuestionDefinition(type, qname);
        int actual = 0;
        for (AuthorityQueryCall call : calls) {
            if (!authorityEndpoint.equals(call.authorityEndpoint())) {
                continue;
            }
            Record question = call.query() != null ? call.query().getQuestion() : null;
            if (expectedQuestion.matches(question)) {
                actual++;
            }
        }
        assertEquals(expectedCount, actual);
    }

    private RecursiveAuthorityScenarioEntry findEntry(AuthorityEndpoint authorityEndpoint, Record question) {
        for (RecursiveAuthorityScenarioEntry entry : entries) {
            if (!entry.getAuthorityEndpoint().equals(authorityEndpoint)) {
                continue;
            }
            if (entry.getQuestionDefinition().matches(question)) {
                return entry;
            }
        }
        return null;
    }
}
