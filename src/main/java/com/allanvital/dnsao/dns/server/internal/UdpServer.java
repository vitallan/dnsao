package com.allanvital.dnsao.dns.server.internal;

import com.allanvital.dnsao.dns.remote.QueryProcessor;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.utils.ExceptionUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UdpServer extends ProtocolServer {

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

                running = true;
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buffer = new byte[4096];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
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

            long start = System.nanoTime();
            DnsQuery dnsQuery = processor.processQuery(clientAddress, data);
            byte[] responseBytes = dnsQuery.getMessageBytes();
            long elapsedNanos = System.nanoTime() - start;
            long elapsedMillis = elapsedNanos / 1_000_000;
            buildAndFireQueryEvent(dnsQuery, elapsedMillis);

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

}