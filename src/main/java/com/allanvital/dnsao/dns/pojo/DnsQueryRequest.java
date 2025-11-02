package com.allanvital.dnsao.dns.pojo;

import org.xbill.DNS.Message;

import java.net.InetAddress;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsQueryRequest {

    private InetAddress clientAddress;
    private Message originalRequest;
    private Message request;
    private final long start;
    private boolean isInternalOrigin = false;

    public DnsQueryRequest(InetAddress clientAddress) {
        this.clientAddress = clientAddress;
        this.start = System.nanoTime();
    }

    public Message getRequest() {
        return request;
    }

    public void setRequest(Message request) {
        this.request = request;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(InetAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public long getStart() {
        return start;
    }

    public boolean isLocalQuery() {
        return isInternalOrigin;
    }

    public void setIsLocalQuery(boolean isInternalOrigin) {
        this.isInternalOrigin = isInternalOrigin;
    }

    public Message getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(Message originalRequest) {
        this.originalRequest = originalRequest;
    }

    public int getOriginalRequestId() {
        return this.originalRequest.getHeader().getID();
    }

}
