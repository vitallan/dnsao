package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.post.handler.json.HttpListenerBodyBuilder;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.eclipsesource.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.QUERY_EVENT_HTTP_NOTIFIED;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HttpListenerPostHandler implements PostHandler {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final HttpListenerBodyBuilder bodyBuilder = new HttpListenerBodyBuilder();
    private final HttpClient client;
    private final Set<String> urlsToNotify;

    public HttpListenerPostHandler(Set<String> urlsToNotify) {
        this.urlsToNotify = urlsToNotify;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        QueryEvent queryEvent = buildQueryEvent(request, response);
        JsonObject bodyToNotify = bodyBuilder.buildHttpListenerBuild(queryEvent);
        for (String url : urlsToNotify) {
            buildAndSendPost(url, bodyToNotify.toString());
        }
        telemetryNotify(QUERY_EVENT_HTTP_NOTIFIED);
    }

    private void buildAndSendPost(String url, String body) {
        HttpRequest post = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            client.send(post, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            log.warn("it was not possible to notify {}. Error: {}", url, e.getMessage());
        }
    }

}
