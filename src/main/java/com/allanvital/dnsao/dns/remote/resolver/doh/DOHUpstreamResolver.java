package com.allanvital.dnsao.dns.remote.resolver.doh;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.Semaphore;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DOHUpstreamResolver implements UpstreamResolver {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final URI uri;
    private final HttpClient client;
    private final Semaphore semaphore;
    private final int port;

    public DOHUpstreamResolver(Upstream upstream) throws NoSuchAlgorithmException, IOException {
        String host = upstream.getHost();
        String path = upstream.getPath();
        if (path == null || path.isEmpty()) {
            path = "/dns-query";
        }
        int port = upstream.getPort();
        if (port == 0) {
            port = 443;
        }
        if (host == null) {
            throw new IOException("for DOH, it is necessary to set host property");
        }
        log.debug("building DOHResolver for host: {}", host);
        this.uri = URI.create("https://" + host + ":" + port + path);
        this.port = port;
        this.semaphore = new Semaphore(64);
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(2))
                .sslContext(SSLContext.getDefault())
                .build();
        log.debug("DOHResolver for host {} built", host);
    }

    @Override
    public String getIp() {
        return uri.getHost();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Message send(Message query) throws IOException {
        HttpRequest request = buildPost(query);
        try {
            semaphore.acquire();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("no 200 response from DohSource: " + getIp());
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase().startsWith("application/dns-message")) {
                throw new IOException("Unexpected Content-Type: " + contentType + " for DohSource: " + getIp());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new IOException("Empty DoH body for DohSource: " + getIp());
            }
            return new Message(body);
        } catch (IOException e) {
            log.debug("failed to execute http request on {}: {}", uri, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            log.trace("request discarded because other returned faster {}", query.getQuestion().getName());
            return null;
        } finally {
            semaphore.release();
        }
    }

    private HttpRequest buildPost(Message query) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/dns-message")
                .header("Accept", "application/dns-message")
                .header("Cache-Control", "no-cache")
                .POST(HttpRequest.BodyPublishers.ofByteArray(query.toWire()))
                .build();
    }

}
