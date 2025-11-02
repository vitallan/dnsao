package com.allanvital.dnsao.graph.bean;

import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FakeDnsServer {

    private final DatagramSocket socket;
    private final Thread listenerThread;
    private int port;
    private final AtomicInteger callCounter = new AtomicInteger(0);
    private final Map<DnsQueryKey, Message> mockResponses = new ConcurrentHashMap<>();
    private final AtomicReference<Integer> lastMessageId = new AtomicReference<>(0);

    public FakeDnsServer(int port) throws SocketException {
        this.socket = new DatagramSocket(port, null);
        if (socket.getLocalPort() != this.port) {
            this.port = socket.getLocalPort();
        }
        //and here we go again
        this.listenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    callCounter.incrementAndGet();

                    byte[] response = handleRequest(packet.getData(), packet.getLength());
                    if (response != null) {
                        DatagramPacket responsePacket = new DatagramPacket(
                                response, response.length, packet.getAddress(), packet.getPort());
                        socket.send(responsePacket);
                    }

                } catch (IOException e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        });
    }

    public int getPort() {
        return this.port;
    }

    public void start() {
        listenerThread.start();
    }

    public void stop() {
        socket.close();
    }

    public int getCallCount() {
        return callCounter.get();
    }

    public void clearCallCount() {
        callCounter.set(0);
    }

    public void mockResponse(Message query, Message responseTemplate) {
        mockResponses.put(DnsQueryKey.fromMessage(query), responseTemplate);
    }

    public int getLastRequestId() {
        return lastMessageId.get();
    }

    private byte[] handleRequest(byte[] requestData, int length) {
        try {
            Message request = new Message(requestData);
            lastMessageId.set(request.getHeader().getID());
            DnsQueryKey key = DnsQueryKey.fromMessage(request);

            Message template = mockResponses.get(key);
            if (template == null) return null;

            Message response = template.clone();
            response.getHeader().setID(request.getHeader().getID());

            return response.toWire();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
