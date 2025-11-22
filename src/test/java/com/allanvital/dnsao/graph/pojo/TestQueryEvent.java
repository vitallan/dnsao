package com.allanvital.dnsao.graph.pojo;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestQueryEvent {

    private String requestTime;
    private String queryResolvedBy;
    private String client;
    private String type;
    private String domain;
    private String answer;
    private String source;
    private Long elapsedTimeInMs;

    public String getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    public String getQueryResolvedBy() {
        return queryResolvedBy;
    }

    public void setQueryResolvedBy(String queryResolvedBy) {
        this.queryResolvedBy = queryResolvedBy;
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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getElapsedTimeInMs() {
        return elapsedTimeInMs;
    }

    public void setElapsedTimeInMs(Long elapsedTimeInMs) {
        this.elapsedTimeInMs = elapsedTimeInMs;
    }

}
