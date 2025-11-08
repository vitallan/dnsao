package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import org.xbill.DNS.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class FakeServer {

    protected static final String CERT_PASS = "changeit";
    protected static final String P12_PATH = "dot-certs/server.p12";

    protected int port;
    protected final AtomicInteger callCounter = new AtomicInteger(0);
    private final Map<DnsQueryKey, Message> mockResponses = new ConcurrentHashMap<>();
    private final AtomicReference<Integer> lastMessageId = new AtomicReference<>(0);

    public int getPort() {
        return this.port;
    }

    public int getLastRequestId() {
        return lastMessageId.get();
    }

    public void clearCallCount() {
        callCounter.set(0);
    }

    public int getCallCount() {
        return callCounter.getAcquire();
    }

    public void mockResponse(Message query, Message responseTemplate) {
        mockResponses.put(DnsQueryKey.fromMessage(query), responseTemplate);
    }

    protected Message getMockedResponse(Message request) {
        lastMessageId.set(request.getHeader().getID());
        DnsQueryKey key = DnsQueryKey.fromMessage(request);

        Message template = mockResponses.get(key);
        if (template == null) return null;

        Message response = template.clone();
        response.getHeader().setID(request.getHeader().getID());

        return response;
    }

    abstract public void start() throws Exception;
    abstract public void stop() throws Exception;

}
