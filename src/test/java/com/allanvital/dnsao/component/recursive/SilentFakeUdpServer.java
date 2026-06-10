package com.allanvital.dnsao.component.recursive;

import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SilentFakeUdpServer extends com.allanvital.dnsao.graph.fake.FakeServer {

    private final DatagramSocket socket;
    private final Thread listenerThread;

    public SilentFakeUdpServer(int port) throws SocketException {
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
                        recordRequest(packet.getData(), packet.getLength());
                        callCounter.incrementAndGet();
                    } catch (IOException e) {
                        if (!socket.isClosed()) {
                            throw new RuntimeException(e);
                        }
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

    private void recordRequest(byte[] requestData, int length) throws IOException {
        byte[] exact = new byte[length];
        System.arraycopy(requestData, 0, exact, 0, length);
        getMockedResponse(new Message(exact));
    }
}
