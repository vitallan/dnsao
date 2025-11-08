package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class FakeUdpServer extends FakeServer {

    private final DatagramSocket socket;
    private final Thread listenerThread;

    public FakeUdpServer(int port) throws SocketException {
        this.socket = new DatagramSocket(port, null);
        if (socket.getLocalPort() != this.port) {
            this.port = socket.getLocalPort();
        }
        //and here we go again
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


}
