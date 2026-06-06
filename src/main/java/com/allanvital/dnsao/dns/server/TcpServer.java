package com.allanvital.dnsao.dns.server;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.utils.ExceptionUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TcpServer extends ProtocolServer {


    private volatile ServerSocket serverSocket;

    public TcpServer(ExecutorService threadPool, QueryProcessorFactory factory, int port) {
        super(threadPool, factory, port);
    }

    @Override
    protected Thread buildServerThread() {
        return new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                if (serverSocket.getLocalPort() != TcpServer.this.port) {
                    TcpServer.this.port = serverSocket.getLocalPort();
                }
                running = true;
                this.serverSocket = serverSocket;
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        client.setSoTimeout(1000);
                        client.setTcpNoDelay(true);
                        client.setKeepAlive(true);
                        threadPool.submit(() -> TcpServer.this.handleClientTcp(client, factory.buildQueryProcessor()));
                    } catch (SocketException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                Log.DNS.error("Error on TCP server start: {}", rootCause.getMessage());
            } finally {
                safeCloseSocket();
            }
        });
    }

    @Override
    protected String threadName() {
        return "tcp-server";
    }

    private void handleClientTcp(Socket client, QueryProcessor processor) {
        try (DataInputStream din = new DataInputStream(new BufferedInputStream(client.getInputStream()));
             DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()))) {

            while (true) {

                int b1;
                try {
                    b1 = din.read();
                } catch (SocketTimeoutException ste) {
                    break;
                }
                if (b1 == -1) break;

                int b2 = din.read();
                if (b2 == -1) {
                    Log.DNS.warn("TCP: Incomplete length header from {}", client.getInetAddress());
                    break;
                }

                int length = (b1 << 8) | b2;
                if (length < 12 || length > 65535) {
                    Log.DNS.warn("TCP: Invalid DNS message length {} from {}", length, client.getInetAddress());
                    break;
                }

                byte[] data = new byte[length];
                try {
                    din.readFully(data);
                } catch (EOFException eof) {
                    Log.DNS.warn("TCP: Truncated DNS message from {}. {} bytes", client.getInetAddress(), length);
                    break;
                }

                if (!hasValidDnsHeader(data)) {
                    Log.DNS.warn("TCP: No valid DNS header from {}. {} bytes", client.getInetAddress(), length);
                    break;
                }

                DnsQuery dnsQuery = processor.processExternalQuery(client.getInetAddress(), data);
                byte[] responseBytes = dnsQuery.getMessageBytes();

                if (responseBytes == null) {
                    Log.DNS.warn("TCP: Processor returned null for {}", client.getInetAddress());
                    break;
                }

                dout.writeShort(responseBytes.length);
                dout.write(responseBytes);
                dout.flush();
            }

        } catch (IOException e) {
            Log.DNS.error("TCP: Error handling client {}", client.getInetAddress(), e);
        } finally {
            try { client.close(); } catch (IOException ignore) {}
        }
    }

    private void safeCloseSocket() {
        if (this.serverSocket != null) {
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                Log.DNS.error("failed to close tcp socket {}", e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.serverSocket != null) {
            safeCloseSocket();
            while (!this.serverSocket.isClosed()) {
                Thread.yield();
            }
        }
    }

}