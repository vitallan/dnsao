package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeUdpTcpDnsServer extends FakeServer {

    private final DatagramSocket udpSocket;
    private final ServerSocket tcpSocket;
    private final Thread udpListenerThread;
    private volatile boolean running;
    private ExecutorService tcpPool;
    private final List<DnsQueryKey> receivedUdpQueries = new ArrayList<>();
    private final List<DnsQueryKey> receivedTcpQueries = new ArrayList<>();
    private final List<Message> receivedUdpMessages = new ArrayList<>();
    private final List<Message> receivedTcpMessages = new ArrayList<>();

    public FakeUdpTcpDnsServer(int port) throws IOException {
        this.tcpSocket = new ServerSocket();
        this.tcpSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        this.port = tcpSocket.getLocalPort();
        this.udpSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), this.port));
        this.udpListenerThread = new Thread(this::listenUdp);
    }

    @Override
    public void start() {
        running = true;
        tcpPool = Executors.newCachedThreadPool();
        udpListenerThread.start();
        tcpPool.submit(this::listenTcp);
    }

    @Override
    public void stop() throws Exception {
        running = false;
        clearCallCount();
        udpSocket.close();
        tcpSocket.close();
        if (tcpPool != null) {
            tcpPool.shutdownNow();
            tcpPool.awaitTermination(3, TimeUnit.SECONDS);
        }
        udpListenerThread.join(3000);
        while (!udpSocket.isClosed() || !tcpSocket.isClosed()) {
            Thread.yield();
        }
    }

    @Override
    public void clearReceivedQueries() {
        super.clearReceivedQueries();
        synchronized (receivedUdpQueries) {
            receivedUdpQueries.clear();
        }
        synchronized (receivedTcpQueries) {
            receivedTcpQueries.clear();
        }
        synchronized (receivedUdpMessages) {
            receivedUdpMessages.clear();
        }
        synchronized (receivedTcpMessages) {
            receivedTcpMessages.clear();
        }
    }

    public List<DnsQueryKey> getReceivedUdpQueries() {
        synchronized (receivedUdpQueries) {
            return List.copyOf(receivedUdpQueries);
        }
    }

    public List<DnsQueryKey> getReceivedTcpQueries() {
        synchronized (receivedTcpQueries) {
            return List.copyOf(receivedTcpQueries);
        }
    }

    public List<Message> getReceivedUdpMessages() {
        synchronized (receivedUdpMessages) {
            return List.copyOf(receivedUdpMessages);
        }
    }

    public List<Message> getReceivedTcpMessages() {
        synchronized (receivedTcpMessages) {
            return List.copyOf(receivedTcpMessages);
        }
    }

    private void listenUdp() {
        byte[] buffer = new byte[2048];
        try {
            while (!udpSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    byte[] response = handleUdpRequest(packet.getData(), packet.getLength());
                    if (response != null) {
                        callCounter.incrementAndGet();
                        DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
                        udpSocket.send(responsePacket);
                    }
                } catch (SocketException e) {
                    break;
                } catch (IOException e) {
                    if (!udpSocket.isClosed()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
            clearCallCount();
            udpSocket.close();
        }
    }

    private void listenTcp() {
        try {
            while (running && !tcpSocket.isClosed()) {
                Socket socket = tcpSocket.accept();
                tcpPool.submit(() -> handleTcpClient(socket));
            }
        } catch (IOException ignored) {
        }
    }

    private byte[] handleUdpRequest(byte[] requestData, int length) {
        try {
            byte[] exact = new byte[length];
            System.arraycopy(requestData, 0, exact, 0, length);
            Message request = new Message(exact);
            recordUdpQuery(request);
            Message response = getMockedResponse(request);
            if (response == null) {
                return MessageHelper.buildServfailFrom(request).toWire();
            }
            return response.toWire();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTcpClient(Socket socket) {
        try (Socket client = socket) {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            while (running) {
                int b1 = in.read();
                if (b1 == -1) {
                    break;
                }
                int b2 = in.read();
                if (b2 == -1) {
                    break;
                }
                int len = ((b1 & 0xFF) << 8) | (b2 & 0xFF);
                if (len <= 0 || len > 65535) {
                    break;
                }

                byte[] requestBytes = in.readNBytes(len);
                if (requestBytes.length != len) {
                    break;
                }

                Message request = new Message(requestBytes);
                recordTcpQuery(request);
                Message response = getMockedResponse(request);
                if (response == null) {
                    response = MessageHelper.buildServfailFrom(request);
                }

                byte[] wire = response.toWire();
                callCounter.incrementAndGet();
                out.write((wire.length >>> 8) & 0xFF);
                out.write(wire.length & 0xFF);
                out.write(wire);
                out.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private void recordUdpQuery(Message request) {
        DnsQueryKey key = DnsQueryKey.fromMessage(request);
        synchronized (receivedUdpQueries) {
            receivedUdpQueries.add(key);
        }
        synchronized (receivedUdpMessages) {
            receivedUdpMessages.add(request.clone());
        }
    }

    private void recordTcpQuery(Message request) {
        DnsQueryKey key = DnsQueryKey.fromMessage(request);
        synchronized (receivedTcpQueries) {
            receivedTcpQueries.add(key);
        }
        synchronized (receivedTcpMessages) {
            receivedTcpMessages.add(request.clone());
        }
    }
}
