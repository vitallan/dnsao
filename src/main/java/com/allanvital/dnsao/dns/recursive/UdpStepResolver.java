package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Message;

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
        try {
            socket.setSoTimeout(timeoutSeconds * 1000);
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(queryBytes, queryBytes.length, address, port);
            socket.send(packet);

            byte[] buffer = new byte[65535];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);

            byte[] responseBytes = new byte[responsePacket.getLength()];
            System.arraycopy(responsePacket.getData(), 0, responseBytes, 0, responsePacket.getLength());
            return new Message(responseBytes);
        } finally {
            socket.close();
        }
    }

    private Message sendTcp(Message query) throws IOException {
        byte[] queryBytes = query.toWire();
        Socket socket = new Socket();
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
            socket.close();
        }
    }

}
