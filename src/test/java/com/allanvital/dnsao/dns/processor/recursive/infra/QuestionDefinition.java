package com.allanvital.dnsao.dns.processor.recursive.infra;

import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QuestionDefinition {

    private final int type;
    private final String qname;

    public QuestionDefinition(int type, String qname) {
        this.type = type;
        this.qname = normalize(qname);
    }

    public boolean matches(Record question) {
        if (question == null) {
            return false;
        }
        Name questionName = question.getName();
        return question.getType() == type && questionName != null && qname.equals(questionName.toString());
    }

    private static String normalize(String qname) {
        if (qname.endsWith(".")) {
            return qname;
        }
        return qname + ".";
    }
}
