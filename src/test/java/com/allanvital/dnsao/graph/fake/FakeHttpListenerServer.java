package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.graph.pojo.TestQueryEvent;
import io.javalin.Javalin;

import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeHttpListenerServer {

    private static final String POST_PATH = "/queries";
    private static final String CONTENT_TYPE = "application/json";

    private Javalin app;
    private int port;
    private final Set<TestQueryEvent> receivedEvents = new HashSet<>();
    private TestQueryEvent latestTestQueryEvent = null;

    public void start() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            this.port = socket.getLocalPort();
        }
        app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = CONTENT_TYPE;
            cfg.jetty.defaultPort = this.port;
        });

        app.post(POST_PATH, ctx -> {
            if (ctx.contentType() == null || !Objects.requireNonNull(ctx.contentType()).toLowerCase().startsWith(CONTENT_TYPE)) {
                ctx.status(415).result("unsupported media type");
                return;
            }
            TestQueryEvent testQueryEvent = ctx.bodyAsClass(TestQueryEvent.class);
            latestTestQueryEvent = testQueryEvent;
            receivedEvents.add(testQueryEvent);
        });

        app.start();
    }

    public void stop() throws Exception {
        if (app != null) {
            app.jettyServer().stop();
            app.stop();
        }
        receivedEvents.clear();
    }

    public int getPort() {
        return this.port;
    }

    public String getUrl() {
        return "http://localhost:" + port + POST_PATH;
    }

    public TestQueryEvent getLatestTestQueryEvent() {
        return latestTestQueryEvent;
    }

    public boolean receivedEvent(TestQueryEvent testQueryEvent) {
        return receivedEvents.contains(testQueryEvent);
    }

}
