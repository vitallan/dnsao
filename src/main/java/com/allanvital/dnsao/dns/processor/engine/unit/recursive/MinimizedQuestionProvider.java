package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MinimizedQuestionProvider {

    public List<Message> buildAuthorityDiscoveryQuestions(Message originalQuery) {
        Record originalQuestion = originalQuery.getQuestion();
        String qname = originalQuestion.getName().toString();
        String withoutDot = qname.endsWith(".") ? qname.substring(0, qname.length() - 1) : qname;
        String[] labels = withoutDot.split("\\.");
        if (labels.length < 2) {
            throw new IllegalArgumentException("expected at least 2 labels for minimized root question: " + qname);
        }
        List<Message> questions = new ArrayList<>();
        for (int i = labels.length - 1; i >= 0; i--) {
            String suffix = joinSuffix(labels, i);
            questions.add(buildQuestion(Type.NS, suffix, originalQuestion.getDClass()));
        }
        return questions;
    }

    public Message buildTargetQuestion(Message originalQuery, String qname) {
        Record originalQuestion = originalQuery.getQuestion();
        return buildQuestion(originalQuestion.getType(), qname, originalQuestion.getDClass());
    }

    private Message buildQuestion(int type, String qname, int dclass) {
        try {
            Name name = Name.fromString(qname);
            return Message.newQuery(Record.newRecord(name, type, dclass == 0 ? DClass.IN : dclass));
        } catch (Exception e) {
            throw new IllegalStateException("failed to build minimized query for " + qname, e);
        }
    }

    private String joinSuffix(String[] labels, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < labels.length; i++) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(labels[i]);
        }
        builder.append('.');
        return builder.toString();
    }
}
