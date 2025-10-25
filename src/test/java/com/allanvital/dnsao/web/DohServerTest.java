package com.allanvital.dnsao.web;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverFactory;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPool;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.helper.MessageUtils;
import com.allanvital.dnsao.notification.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.SIMPLE;
import static com.allanvital.dnsao.notification.EventType.QUERY_RESOLVED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DohServerTest extends TestHolder {

    private WebServer webServer;
    private String domain = "example.com";
    private String responseIp = "5.5.5.5";
    private HttpClient client;
    private Message query;

    @BeforeEach
    public void before() throws ConfException {
        super.loadConf("1udp-upstream-cache-web.yml", false);
        super.startFakeDnsServer();
        super.prepareSimpleMockResponse(domain, responseIp, 10000);
        ResolverFactory resolverFactory = new ResolverFactory(null, conf.getUpstreams());
        QueryProcessorFactory queryProcessorFactory = new QueryProcessorFactory(resolverFactory.getAllResolvers(), null, null, 1, SIMPLE);
        webServer = new WebServer(conf.getServer().getWebPort(), queryProcessorFactory, 10);
        webServer.start();
        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        query = MessageUtils.buildARequest("example.com");
    }

    @Test
    public void shouldReturnDnsQueryOnHttp() throws Exception {
        HttpRequest httpRequest = buildPostRequest(query);
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 200);
        Message answer = new Message(response.body());
        String responseIp = MessageUtils.extractIpFromResponseMessage(answer);
        assertEquals(this.responseIp, responseIp);
        eventListener.assertCount(QUERY_RESOLVED, 1);
    }

    @Test
    public void shouldReturnDnsQueryOnHttpWithMultipleAccept() throws Exception {
        HttpRequest httpRequest = buildPostRequest(query, "application/dns-message", "*/*, application/dns-message");
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 200);
        Message answer = new Message(response.body());
        String responseIp = MessageUtils.extractIpFromResponseMessage(answer);
        assertEquals(this.responseIp, responseIp);
        eventListener.assertCount(QUERY_RESOLVED, 1);
    }

    @Test
    public void unnaceptableContentType() throws Exception {
        HttpRequest httpRequest = buildPostRequest(query, "text/plain", "application/dns-message");
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 415);
    }

    @Test
    public void unnaceptableAccept() throws Exception {
        HttpRequest httpRequest = buildPostRequest(query, "application/dns-message", "application/json");
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 406);
    }

    @Test
    public void emptyBody() throws Exception {
        HttpRequest httpRequest = buildPostRequest(null);
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 400);
    }

    @Test
    public void shouldReturnDnsQueryOnHttpGet() throws Exception {
        HttpRequest httpRequest = buildGetRequest(query, true);
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 200);
        Message answer = new Message(response.body());
        assertEquals(this.responseIp, MessageUtils.extractIpFromResponseMessage(answer));
        eventListener.assertCount(QUERY_RESOLVED, 1);
    }

    @Test
    public void missingDnsParamOnGet() throws Exception {
        HttpRequest httpRequest = buildGetRequest(query, false);
        HttpResponse<byte[]> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        assertHttp(response, 400);
    }

    private void assertHttp(HttpResponse<byte[]> response, int status) {
        assertEquals(status, response.statusCode());
        assertNotNull(response.headers().firstValue("Content-Type").orElse(null));
        assertTrue(response.headers().firstValue("Content-Type").get().startsWith("application/dns-message"));
    }

    private HttpRequest buildPostRequest(Message query) {
        return buildPostRequest(query, "application/dns-message", "application/dns-message");
    }

    private HttpRequest buildPostRequest(Message query, String contentType, String accept) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + webServer.getPort() + "/dns-query"))
                .header("Content-Type", contentType)
                .header("Accept", accept)
                .POST(HttpRequest.BodyPublishers.ofByteArray((query == null) ? (new byte[0]) : (query.toWire())))
                .build();
    }

    private HttpRequest buildGetRequest(Message query, boolean addParameter) {
        String b64 = "?dns=" + Base64.getUrlEncoder().withoutPadding().encodeToString(query.toWire());
        if (!addParameter) {
            b64 = "";
        }
        return HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + webServer.getPort() + "/dns-query" + b64))
                .header("Accept", "application/dns-message")
                .GET()
                .build();
    }

    @AfterEach
    public void tearDown() {
        super.stopFakeDnsServer();
        webServer.stop();
        eventListener.reset();
    }

}
