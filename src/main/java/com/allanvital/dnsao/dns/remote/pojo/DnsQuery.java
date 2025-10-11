package com.allanvital.dnsao.dns.remote.pojo;

import com.allanvital.dnsao.notification.QueryResolvedBy;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsQuery {

    private String client;
    private String source;
    private Message response;
    private QueryResolvedBy queryResolvedBy;

    public DnsQuery(String client) {
        this.client = client;
    }

    public void setResolvedBySourceAndResponse(QueryResolvedBy queryResolvedBy, String source, Message response) {
        this.source = source;
        this.setResolvedByAndResponse(queryResolvedBy, response);
    }

    public void setResolvedByAndResponse(QueryResolvedBy queryResolvedBy, Message response) {
        this.queryResolvedBy = queryResolvedBy;
        this.response = response;
    }

    public byte[] getMessageBytes() {
        if (response == null) {
            return null;
        }
        return response.toWire();
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Message getResponse() {
        return response;
    }

    public void setResponse(Message response) {
        this.response = response;
    }

    public QueryResolvedBy getQueryResolvedBy() {
        return queryResolvedBy;
    }

    public void setQueryResolvedBy(QueryResolvedBy queryResolvedBy) {
        this.queryResolvedBy = queryResolvedBy;
    }
}