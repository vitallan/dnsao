package com.allanvital.dnsao.dns.server;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.utils.ExceptionUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UdpServer extends ProtocolServer {

    private volatile DatagramSocket socket;

    public UdpServer(ExecutorService threadPool, QueryProcessorFactory factory, int port) {
        super(threadPool, factory, port);
    }

    @Override
    protected Thread buildServerThread() {
        return new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(port)) {
                if (socket.getLocalPort() != this.port) {
                    this.port = socket.getLocalPort();
                }
                this.socket = socket;
                running = true;
                while (running) {
                    byte[] buffer = new byte[4096];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketException e) {
                        log.debug("stopping udpServer because of socketException");
                        running = false;
                        break;
                    }
                    int len = packet.getLength();
                    int off = packet.getOffset();
                    byte[] data = Arrays.copyOfRange(packet.getData(), off, off + len);
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
                    threadPool.submit(() -> handleClientUdp(socket, address, port, data, factory.buildQueryProcessor()));
                }
            } catch (IOException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                log.error("Error on UDP server start: {}", rootCause.getMessage());
            } finally {
                if (this.socket != null && !this.socket.isClosed()) {
                    this.socket.close();
                }
            }
        });
    }

    private void handleClientUdp(DatagramSocket socket, InetAddress clientAddress, int clientPort, byte[] data, QueryProcessor processor) {
        try {
            if (data.length < 12) {
                log.warn("UDP: DNS message too short ({} bytes) from {}:{}", data.length, clientAddress, clientPort);
                return;
            }
            if (data.length > 512) {
                log.warn("UDP: Oversized DNS message ({} bytes) from {}:{}", data.length, clientAddress, clientPort);
            }

            if (!hasValidDnsHeader(data)) {
                log.warn("UDP: DNS message failed the header check ({} bytes) from {}", data.length, clientAddress);
                return;
            }

            DnsQuery dnsQuery = processor.processExternalQuery(clientAddress, data);
            byte[] responseBytes = dnsQuery.getMessageBytes();

            if (responseBytes == null) {
                log.warn("UDP: Processor returned null for {}:{}", clientAddress, clientPort);
                return;
            }

            DatagramPacket responsePacket = new DatagramPacket(
                    responseBytes, responseBytes.length,
                    clientAddress, clientPort
            );
            socket.send(responsePacket);

        } catch (IOException e) {
            log.error("Error trying to handle UDP client", e);
        }
    }

    @Override
    protected String threadName() {
        return "udp-server";
    }

    @Override
    public void stop() {
        super.stop();
        if (this.socket != null) {
            this.socket.close();
            while (!this.socket.isClosed()) {
                Thread.yield();
            }
        }
    }

}