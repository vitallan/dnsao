package com.allanvital.dnsao.notification;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;

import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryEvent {

    private long time;
    private QueryResolvedBy queryResolvedBy;
    private String client;
    private String type;
    private String domain;
    private String answer;
    private String source;
    private long elapsedTime;

    public QueryEvent(long time) {
        this.time = time;
    }

    public QueryEvent(QueryResolvedBy queryResolvedBy) {
        this.queryResolvedBy = queryResolvedBy;
    }

    public QueryEvent(QueryResolvedBy resolvedBy, String source, long time) {
        this(resolvedBy);
        this.time = time;
        this.source = source;
    }

    public QueryEvent(String client, Message message, String source, QueryResolvedBy resolvedBy, long elapsedTime) {
        this(resolvedBy);
        Record question = message.getQuestion();
        String domain = question.getName().toString();
        String typeName = Type.string(question.getType());
        StringBuilder result = new StringBuilder();
        String comma = "";
        List<Record> answers = message.getSection(Section.ANSWER);
        for (Record r : answers) {
            if (r.getType() == Type.A) {
                ARecord a = (ARecord) r;
                result.append(comma).append(a.getAddress().getHostAddress());
            } else if (r.getType() == Type.AAAA) {
                AAAARecord aaaa = (AAAARecord) r;
                result.append(comma).append(aaaa.getAddress().getHostAddress());
            } else if (r.getType() == Type.CNAME) {
                CNAMERecord c = (CNAMERecord) r;
                result.append(comma).append(c.getTarget());
            }
            comma = ",";
        }
        this.type = typeName;
        this.domain = domain;
        this.client = client;
        this.answer = result.toString();
        this.source = source;
        this.elapsedTime = elapsedTime;
        this.time = System.currentTimeMillis();
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    @Override
    public String toString() {
        return "QueryEvent {" +
                "time=" + formatMillis(time, "HH:mm:ss.SSS") +
                ", client='" + client + '\'' +
                ", type='" + type + '\'' +
                ", domain='" + domain + '\'' +
                ", answer='" + answer + '\'' +
                ", source='" + source + '\'' +
                ", queryResolvedBy='" + queryResolvedBy + '\'' +
                ", elapsedTime=" + elapsedTime +
                '}';
    }

    public QueryResolvedBy getQueryResolvedBy() {
        return queryResolvedBy;
    }

    public void setQueryResolvedBy(QueryResolvedBy queryResolvedBy) {
        this.queryResolvedBy = queryResolvedBy;
    }
}