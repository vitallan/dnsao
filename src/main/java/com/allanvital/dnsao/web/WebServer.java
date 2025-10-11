package com.allanvital.dnsao.web;

import com.allanvital.dnsao.web.json.JsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class WebServer {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private Javalin app;
    private final int port;
    private final StatsCollector statsCollector = new StatsCollector();
    private final JsonBuilder builder = new JsonBuilder(statsCollector);

    public WebServer(int port) {
        this.port = port;
    }

    public void start() {
        if (port == -1) {
            log.debug("web server disabled, using webPort = -1");
            return;
        }
        log.debug("starting web server on port {}", port);
        this.app = Javalin.create(cfg -> {
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location  = Location.CLASSPATH;
            });
            cfg.router.ignoreTrailingSlashes = true;
        });

        app.get("/stats", ctx -> {
            ctx.contentType("application/json; charset=utf-8").result(
                    builder.buildJsonStats().toString()
            );
        });

        app.start(port);

        log.debug("web server running on port {}", port);
    }

    public void stop() {
        log.debug("stopping web server");
        if (app != null) {
            app.stop();
        }
        log.debug("web server stopped");
    }

}