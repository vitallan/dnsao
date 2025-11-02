package com.allanvital.dnsao.dns.server;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TcpServer extends ProtocolServer {

    private static final Logger log = LoggerFactory.getLogger(DNS);

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
                while (!Thread.currentThread().isInterrupted()) {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(1000);
                    client.setTcpNoDelay(true);
                    client.setKeepAlive(true);
                    threadPool.submit(() -> TcpServer.this.handleClientTcp(client, factory.buildQueryProcessor()));
                }
            } catch (IOException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                log.error("Error on TCP server start: {}", rootCause.getMessage());
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
                    log.warn("TCP: Incomplete length header from {}", client.getInetAddress());
                    break;
                }

                int length = (b1 << 8) | b2;
                if (length < 12 || length > 65535) {
                    log.warn("TCP: Invalid DNS message length {} from {}", length, client.getInetAddress());
                    break;
                }

                byte[] data = new byte[length];
                try {
                    din.readFully(data);
                } catch (EOFException eof) {
                    log.warn("TCP: Truncated DNS message from {}. {} bytes", client.getInetAddress(), length);
                    break;
                }

                if (!hasValidDnsHeader(data)) {
                    log.warn("TCP: No valid DNS header from {}. {} bytes", client.getInetAddress(), length);
                    break;
                }

                DnsQuery dnsQuery = processor.processQuery(client.getInetAddress(), data);
                byte[] responseBytes = dnsQuery.getMessageBytes();

                if (responseBytes == null) {
                    log.warn("TCP: Processor returned null for {}", client.getInetAddress());
                    break;
                }

                dout.writeShort(responseBytes.length);
                dout.write(responseBytes);
                dout.flush();
            }

        } catch (IOException e) {
            log.error("TCP: Error handling client {}", client.getInetAddress(), e);
        } finally {
            try { client.close(); } catch (IOException ignore) {}
        }
    }

}