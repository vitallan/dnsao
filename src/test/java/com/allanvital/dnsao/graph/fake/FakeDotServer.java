package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeDotServer extends FakeServer {

    private boolean running = false;
    private ExecutorService pool;
    private SSLServerSocket serverSocket;

    @Override
    public void start() throws Exception {
        running = true;
        pool = Executors.newCachedThreadPool();
        SSLContext context = buildServerSslContext();
        SSLServerSocketFactory factory = context.getServerSocketFactory();
        try (ServerSocket plain = new ServerSocket()) {
            plain.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
            this.port = plain.getLocalPort();
        }
        this.serverSocket = (SSLServerSocket) factory.createServerSocket();
        this.serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.setNeedClientAuth(false);
        this.serverSocket.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
        pool.submit(() -> {
            while (running && !serverSocket.isClosed()) {
                try {
                    final SSLSocket client = (SSLSocket) serverSocket.accept();
                    pool.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void stop() throws InterruptedException {
        running = false;
        clearCallCount();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {

        }
        pool.shutdown();
    }

    private void handleClient(SSLSocket socket) {
        try (SSLSocket sslSocket = socket) {
            sslSocket.startHandshake();

            InputStream in = sslSocket.getInputStream();
            OutputStream out = sslSocket.getOutputStream();

            while (running) {
                int b1 = in.read();
                if (b1 == -1) break;
                int b2 = in.read();
                if (b2 == -1) break;
                int len = ((b1 & 0xFF) << 8) | (b2 & 0xFF);
                if (len <= 0 || len > 65535) break;

                byte[] req = in.readNBytes(len);
                if (req.length != len) break;

                byte[] byteResponse;
                Message request = new Message(req);
                Message response = null;
                Message mockedResponse = super.getMockedResponse(request);
                if (mockedResponse != null) {
                    response = mockedResponse;
                } else {
                    response = MessageHelper.buildServfailFrom(request);
                }
                byteResponse = response.toWire();

                //dot standards
                out.write((byteResponse.length >>> 8) & 0xFF);
                out.write(byteResponse.length & 0xFF);
                out.write(byteResponse);
                out.flush();
            }
        } catch (IOException ignored) {

        }
    }

    private static SSLContext buildServerSslContext() throws Exception {
        char[] passwordCharArray = CERT_PASS.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(P12_PATH)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + P12_PATH);
            ks.load(is, passwordCharArray);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwordCharArray);

        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }

}
