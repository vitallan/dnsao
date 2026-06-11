package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import org.xbill.DNS.Message;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
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
    private final Map<DnsQueryKey, Message> repeatedResponses = new ConcurrentHashMap<>();
    private final Map<DnsQueryKey, Deque<Message>> chainedResponses = new ConcurrentHashMap<>();
    private final List<DnsQueryKey> receivedQueries = new ArrayList<>();
    private final List<Message> receivedMessages = new ArrayList<>();
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

    public List<DnsQueryKey> getReceivedQueries() {
        synchronized (receivedQueries) {
            return List.copyOf(receivedQueries);
        }
    }

    public List<Message> getReceivedMessages() {
        synchronized (receivedMessages) {
            return List.copyOf(receivedMessages);
        }
    }

    public void clearReceivedQueries() {
        synchronized (receivedQueries) {
            receivedQueries.clear();
        }
        synchronized (receivedMessages) {
            receivedMessages.clear();
        }
    }

    public void mockResponse(Message query, Message responseTemplate) {
        DnsQueryKey key = DnsQueryKey.fromMessage(query);
        repeatedResponses.put(key, responseTemplate);
        chainedResponses.remove(key);
    }

    public void mockResponseChain(Message query, Message... responseTemplates) {
        DnsQueryKey key = DnsQueryKey.fromMessage(query);
        Deque<Message> queue = new ArrayDeque<>();
        for (Message responseTemplate : responseTemplates) {
            queue.addLast(responseTemplate);
        }
        chainedResponses.put(key, queue);
    }

    protected synchronized Message getMockedResponse(Message request) {
        lastMessageId.set(request.getHeader().getID());
        DnsQueryKey key = DnsQueryKey.fromMessage(request);
        synchronized (receivedQueries) {
            receivedQueries.add(key);
        }
        synchronized (receivedMessages) {
            receivedMessages.add(request.clone());
        }

        Deque<Message> chain = chainedResponses.get(key);
        if (chain != null) {
            Message chainedTemplate = chain.pollFirst();
            if (chain.isEmpty()) {
                chainedResponses.remove(key);
            }
            if (chainedTemplate != null) {
                return cloneWithIncomingId(chainedTemplate, request);
            }
        }

        Message template = repeatedResponses.get(key);
        if (template == null) return null;

        return cloneWithIncomingId(template, request);
    }

    private Message cloneWithIncomingId(Message template, Message request) {
        Message response = template.clone();
        response.getHeader().setID(request.getHeader().getID());
        return response;
    }

    abstract public void start() throws Exception;
    abstract public void stop() throws Exception;

}
