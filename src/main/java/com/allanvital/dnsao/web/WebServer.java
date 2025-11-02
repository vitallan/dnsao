package com.allanvital.dnsao.web;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.web.json.JsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Objects;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;
import static java.net.InetAddress.getByName;
import static java.util.Base64.getUrlDecoder;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private Javalin app;
    private int port;
    private final QueryProcessorFactory queryProcessorFactory;
    private final int httpThreadPool;
    private final StatsCollector statsCollector = new StatsCollector();
    private final JsonBuilder builder = new JsonBuilder(statsCollector);
    private static final String DNS_PATH = "/dns-query";
    private static final String CONTENT_TYPE = "application/dns-message";
    private boolean running = false;

    public WebServer(int port, QueryProcessorFactory queryProcessorFactory, int httpThreadPool) {
        this.port = port;
        this.queryProcessorFactory = queryProcessorFactory;
        this.httpThreadPool = httpThreadPool;
    }

    public void start() {
        if (port == -1) {
            log.debug("web server disabled, using webPort = -1");
            return;
        }
        log.debug("starting web server on port {}", port);
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("web");
        threadPool.setMaxThreads(httpThreadPool);
        threadPool.setMinThreads(httpThreadPool);
        this.app = Javalin.create(cfg -> {
            cfg.jetty.threadPool = threadPool;
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location  = Location.CLASSPATH;
            });
            cfg.router.ignoreTrailingSlashes = true;
        });

        app.get("/query", ctx -> ctx.redirect("/query.html"));

        app.get("/stats", ctx -> {
            ctx.contentType("application/json; charset=utf-8").result(
                    builder.buildHomeJsonStats().toString()
            );
        });

        app.get("/queries", ctx -> {
            ctx.contentType("application/json; charset=utf-8").result(
                    builder.buildQueriesArray().toString()
            );
        });
        
        mapDnsEndpoints();

        this.app.start(port);
        this.port = this.app.port();

        log.debug("web server running on port {}", port);
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    class ErrorResult {
        int status;
        String message;

        public ErrorResult(int status, String message) {
            this.status = status;
            this.message = message;
        }

    }

    private ErrorResult validateBasic(Context context, String parameter) {
        String accept = context.header("accept");
        if (accept != null && !accept.contains(CONTENT_TYPE)) {
            return new ErrorResult(406, "non valid accept header");
        }
        if (parameter == null || parameter.isEmpty()) {
            return new ErrorResult(400, "empty dns query");
        }
        return null;
    }

    private void mapDnsEndpoints() {
        app.post(DNS_PATH, ctx -> {
            ctx = ctx.contentType(CONTENT_TYPE);
            String body = ctx.body();
            ErrorResult errorResult = validateBasic(ctx, body);
            if (errorResult != null) {
                ctx.status(errorResult.status).result(errorResult.message);
                return;
            }
            if (ctx.contentType() == null || !Objects.requireNonNull(ctx.contentType()).toLowerCase().startsWith(CONTENT_TYPE)) {
                ctx.status(415).result("unsupported media type");
                return;
            }
            byte[] requestBytes = ctx.bodyAsBytes();

            processQueryAndSetResult(ctx, requestBytes);
        });

        app.get(DNS_PATH, ctx -> {
            ctx = ctx.contentType(CONTENT_TYPE);
            String encoded = ctx.queryParam("dns");
            ErrorResult errorResult = validateBasic(ctx, encoded);
            if (errorResult != null) {
                ctx.status(errorResult.status).result(errorResult.message);
                return;
            }

            byte[] requestBytes;
            try {
                requestBytes = getUrlDecoder().decode(encoded);
            } catch (IllegalArgumentException e) {
                ctx.status(400).result("invalid dns param");
                return;
            }

            processQueryAndSetResult(ctx, requestBytes);

        });
    }

    private void processQueryAndSetResult(Context ctx, byte[] request) throws UnknownHostException {
        String ip = getIp(ctx);
        QueryProcessor processor = queryProcessorFactory.buildQueryProcessor();
        DnsQuery dnsQuery = processor.processQuery(getByName(ip), request);
        byte[] responseBytes = dnsQuery.getMessageBytes();

        ctx.status(200).result(responseBytes);
    }

    private String getIp(Context context) {
        String ip = context.header("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = context.header("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = context.req().getRemoteAddr();
        }
        return ip;
    }

    public int getPort() {
        return this.port;
    }

    public void stop() {
        log.debug("stopping web server");
        if (app != null) {
            app.stop();
        }
        running = false;
        log.debug("web server stopped");
    }

}