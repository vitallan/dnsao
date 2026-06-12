package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Message;
import org.xbill.DNS.Record;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UdpStepResolver implements StepResolver {

    private final String ip;
    private final int port;
    private final int timeoutSeconds;
    private volatile DatagramSocket activeUdpSocket;
    private volatile Socket activeTcpSocket;

    public UdpStepResolver(String ip, int port, int timeoutSeconds) {
        this.ip = ip;
        this.port = port;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public StepResponse send(StepRequest request) {
        Message query = request.toWireMessage();
        try {
            Message udpResponse = sendUdp(query);
            if (udpResponse == null) {
                return null;
            }
            StepResponse stepResponse = new StepResponse(udpResponse);
            if (!stepResponse.isTruncated()) {
                return stepResponse;
            }

            Message tcpResponse = sendTcp(query);
            if (tcpResponse == null) {
                return null;
            }
            return new StepResponse(tcpResponse);
        } catch (IOException e) {
            return null;
        }
    }

    private Message sendUdp(Message query) throws IOException {
        byte[] queryBytes = query.toWire();
        DatagramSocket socket = new DatagramSocket();
        activeUdpSocket = socket;
        try {
            InetAddress address = InetAddress.getByName(ip);
            socket.connect(address, port);
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            DatagramPacket packet = new DatagramPacket(queryBytes, queryBytes.length, address, port);
            socket.send(packet);

            while (!socket.isClosed()) {
                long remainingMs = deadline - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    return null;
                }
                socket.setSoTimeout((int) remainingMs);

                byte[] buffer = new byte[65535];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(responsePacket);

                byte[] responseBytes = new byte[responsePacket.getLength()];
                System.arraycopy(responsePacket.getData(), responsePacket.getOffset(), responseBytes, 0, responsePacket.getLength());
                Message response = new Message(responseBytes);
                if (isValidReply(query, response)) {
                    return response;
                }
            }
            return null;
        } finally {
            activeUdpSocket = null;
            socket.close();
        }
    }

    private Message sendTcp(Message query) throws IOException {
        byte[] queryBytes = query.toWire();
        Socket socket = new Socket();
        activeTcpSocket = socket;
        try {
            socket.connect(new InetSocketAddress(ip, port), timeoutSeconds * 1000);
            socket.setSoTimeout(timeoutSeconds * 1000);

            socket.getOutputStream().write((queryBytes.length >>> 8) & 0xFF);
            socket.getOutputStream().write(queryBytes.length & 0xFF);
            socket.getOutputStream().write(queryBytes);
            socket.getOutputStream().flush();

            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            int responseLength = dataIn.readUnsignedShort();
            byte[] responseBytes = new byte[responseLength];
            dataIn.readFully(responseBytes);

            return new Message(responseBytes);
        } finally {
            activeTcpSocket = null;
            socket.close();
        }
    }

    private boolean isValidReply(Message query, Message response) {
        if (!response.getHeader().getFlag(org.xbill.DNS.Flags.QR)) {
            return false;
        }
        if (response.getHeader().getID() != query.getHeader().getID()) {
            return false;
        }
        if (response.getHeader().getOpcode() != query.getHeader().getOpcode()) {
            return false;
        }
        Record queryQuestion = query.getQuestion();
        Record responseQuestion = response.getQuestion();
        if (queryQuestion == null || responseQuestion == null) {
            return false;
        }
        if (!queryQuestion.getName().equals(responseQuestion.getName())) {
            return false;
        }
        if (queryQuestion.getType() != responseQuestion.getType()) {
            return false;
        }
        return queryQuestion.getDClass() == responseQuestion.getDClass();
    }

    @Override
    public void close() {
        DatagramSocket udpSocket = activeUdpSocket;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        Socket tcpSocket = activeTcpSocket;
        if (tcpSocket != null && !tcpSocket.isClosed()) {
            try {
                tcpSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

}
