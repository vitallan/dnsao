package com.allanvital.dnsao.component;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardAuthTest extends TestHolder {

    private HttpClient httpClient;
    private int webPort;

    @BeforeEach
    void setup() throws Exception {
        safeStart("auth-test.yml");
        webPort = dnsServer.getHttpPort();
        httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .build();
    }

    @Test
    void shouldRejectWithoutAuth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + webPort + "/api/state"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + webPort + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"wrong\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldAllowAfterLogin() throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + webPort + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"secret123\"}"))
                .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResponse.statusCode());

        HttpRequest stateRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + webPort + "/api/state"))
                .GET()
                .build();

        HttpResponse<String> stateResponse = httpClient.send(stateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, stateResponse.statusCode());
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        safeStop();
    }

}
