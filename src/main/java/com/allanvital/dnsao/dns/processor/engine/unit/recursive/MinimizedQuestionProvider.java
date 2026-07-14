package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MinimizedQuestionProvider {

    public Message buildRootQuestion(Message originalQuery) {
        Record originalQuestion = originalQuery.getQuestion();
        String qname = originalQuestion.getName().toString();
        String withoutDot = qname.endsWith(".") ? qname.substring(0, qname.length() - 1) : qname;
        String[] labels = withoutDot.split("\\.");
        if (labels.length < 2) {
            throw new IllegalArgumentException("expected at least 2 labels for minimized root question: " + qname);
        }
        String topLevelDomain = labels[labels.length - 1] + ".";
        return buildQuestion(Type.NS, topLevelDomain, originalQuestion.getDClass());
    }

    public Message buildAuthorityDiscoveryQuestion(Message originalQuery) {
        Record originalQuestion = originalQuery.getQuestion();
        return buildQuestion(Type.NS, originalQuestion.getName().toString(), originalQuestion.getDClass());
    }

    private Message buildQuestion(int type, String qname, int dclass) {
        try {
            Name name = Name.fromString(qname);
            return Message.newQuery(Record.newRecord(name, type, dclass == 0 ? DClass.IN : dclass));
        } catch (Exception e) {
            throw new IllegalStateException("failed to build minimized query for " + qname, e);
        }
    }
}
