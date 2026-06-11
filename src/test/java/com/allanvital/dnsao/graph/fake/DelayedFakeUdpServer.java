package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class DelayedFakeUdpServer extends FakeServer {

    private final DatagramSocket socket;
    private final Thread listenerThread;
    private final long delayMs;

    public DelayedFakeUdpServer(int port, long delayMs) throws SocketException {
        this.delayMs = delayMs;
        this.socket = new DatagramSocket(port, null);
        if (socket.getLocalPort() != this.port) {
            this.port = socket.getLocalPort();
        }
        this.listenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            try {
                while (!socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        try {
                            socket.receive(packet);
                        } catch (SocketException e) {
                            break;
                        }

                        byte[] response = handleRequest(packet.getData(), packet.getLength());
                        if (response != null) {
                            callCounter.incrementAndGet();
                            sleep(delayMs);
                            DatagramPacket responsePacket = new DatagramPacket(
                                    response, response.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                        }

                    } catch (IOException e) {
                        if (!socket.isClosed()) e.printStackTrace();
                    }
                }
            } finally {
                clearCallCount();
                socket.close();
            }
        });
    }

    @Override
    public void start() {
        listenerThread.start();
    }

    @Override
    public void stop() {
        clearCallCount();
        socket.close();
        while (!socket.isClosed()) {
            Thread.yield();
        }
    }

    private byte[] handleRequest(byte[] requestData, int length) {
        try {
            Message request = new Message(requestData);
            Message response = getMockedResponse(request);
            if (response == null) {
                return MessageHelper.buildServfailFrom(request).toWire();
            }
            return response.toWire();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
