package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import org.xbill.DNS.Message;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeDohServer extends FakeServer {

    private static final String DNS_PATH = "/dns-query";

    private Javalin app;

    @Override
    public void start() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.port = socket.getLocalPort();
        }
        SslPlugin ssl = buildSslPlugin(this.port);
        app = Javalin.create(cfg -> {
            cfg.registerPlugin(ssl);
            cfg.http.defaultContentType = "application/dns-message";
        });

        app.post(DNS_PATH, ctx -> {
            byte[] query = ctx.bodyAsBytes();
            Message request = new Message(query);
            ctx.result(getResponseOrServfail(request).toWire());
        });

        app.get(DNS_PATH, ctx -> {
            byte[] query = ctx.bodyAsBytes();
            Message request = new Message(query);
            ctx.result(getResponseOrServfail(request).toWire());
        });

        app.start();
    }

    private Message getResponseOrServfail(Message request) {
        Message response = getMockedResponse(request);
        if (response != null) {
            return response;
        }
        return MessageHelper.buildServfailFrom(request);
    }

    @Override
    public void stop() throws Exception {
        if (app != null) {
            app.jettyServer().stop();
            app.stop();
        }
    }

    private SslPlugin buildSslPlugin(int httpsPort) throws IOException {
        byte[] p12Bytes;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(P12_PATH)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + P12_PATH);
            p12Bytes = is.readAllBytes();
        }
        return new SslPlugin(sslConf -> {
            sslConf.keystoreFromInputStream(new ByteArrayInputStream(p12Bytes), CERT_PASS);
            sslConf.insecure = false;
            sslConf.secure = true;
            sslConf.securePort = httpsPort;
        });
    }

}
