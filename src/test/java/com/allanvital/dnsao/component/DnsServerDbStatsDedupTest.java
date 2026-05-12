package com.allanvital.dnsao.component;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DnsServerDbStatsDedupTest extends TestHolder {

    @TempDir
    Path tempDir;

    private HttpClient client;

    @BeforeEach
    public void setup() throws ConfException {
        loadConf("1udp-upstream-cache-web.yml");

        conf.getMisc().setQueryLog(true);
        conf.getServer().setStatsDbPath(tempDir.resolve("stats.sqlite").toString());

        safeStartWithPresetConf();

        client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    @Test
    public void shouldCountSingleQueryExactlyOnce() throws Exception {
        String domain = "example.com";
        prepareSimpleMockResponse(domain, "10.10.10.10", 10_000);

        SimpleResolver resolver = buildResolver(dnsServer.getUdpPort());
        Message req = MessageHelper.buildARequest(domain);
        resolver.setTimeout(Duration.ofSeconds(3));
        resolver.send(req);

        awaitStatsTotal(1, Duration.ofSeconds(6));
    }

    @Test
    public void shouldCountMultipleSequentialQueriesExactlyOnceEach() throws Exception {
        String domain = "example.com";
        prepareSimpleMockResponse(domain, "10.10.10.10", 10_000);

        int n = 50;
        SimpleResolver resolver = buildResolver(dnsServer.getUdpPort());
        resolver.setTimeout(Duration.ofSeconds(3));

        for (int i = 0; i < n; i++) {
            resolver.send(MessageHelper.buildARequest(domain));
        }

        awaitStatsTotal(n, Duration.ofSeconds(10));
    }

    @Test
    public void shouldCountConcurrentQueriesExactlyOnceEach() throws Exception {
        List<String> domains = List.of("a.example.com", "b.example.com", "c.example.com", "d.example.com");
        for (int i = 0; i < domains.size(); i++) {
            prepareSimpleMockResponse(domains.get(i), "10.10.10." + (10 + i), 10_000);
        }

        int n = 200;
        int threads = 8;

        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                final String domain = domains.get(i % domains.size());
                futures.add(exec.submit(() -> {
                    SimpleResolver r = buildResolver(dnsServer.getUdpPort());
                    r.setTimeout(Duration.ofSeconds(3));
                    r.send(MessageHelper.buildARequest(domain));
                    return null;
                }));
            }

            for (Future<Void> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            exec.shutdownNow();
        }

        awaitStatsTotal(n, Duration.ofSeconds(15));
    }

    private void awaitStatsTotal(long expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        long maxSeen = 0;
        while (System.nanoTime() < deadline) {
            long total = readStatsTotal();
            maxSeen = Math.max(maxSeen, total);
            if (total == expected) {
                return;
            }
            if (total > expected) {
                Assertions.fail("/stats summary.total exceeded expected. expected=" + expected + " actual=" + total + " maxSeen=" + maxSeen);
            }
            Thread.sleep(50);
        }
        Assertions.fail("/stats summary.total did not reach expected within timeout. expected=" + expected + " maxSeen=" + maxSeen);
    }

    private long readStatsTotal() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + dnsServer.getHttpPort() + "/stats"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject root = Json.parse(resp.body()).asObject();
        return root.get("summary").asObject().getLong("total", -1);
    }
}
