package com.allanvital.dnsao.component;

import com.allanvital.dnsao.conf.inner.HttpListenerConf;
import com.allanvital.dnsao.graph.fake.FakeHttpListenerServer;
import com.allanvital.dnsao.graph.pojo.TestQueryEvent;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.QUERY_EVENT_HTTP_NOTIFIED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HttpListenerTest extends TestHolder {

    private final FakeHttpListenerServer httpListener = new FakeHttpListenerServer();
    private static final String domain = "example.com";
    private static final String ip = "10.10.10.10";

    @BeforeEach
    public void setup() throws Exception {
        httpListener.start();
        loadConf("1udp-upstream-nocache-listener.yml");
        fixHttpListenerUrls();
        safeStartWithPresetConf();
        prepareSimpleMockResponse(domain, ip);
    }

    @Test
    public void testSimpleListener() throws Exception {
        super.executeRequestOnOwnServer(domain);
        eventListener.assertCount(QUERY_EVENT_HTTP_NOTIFIED, 1, false);
        TestQueryEvent queryEvent = httpListener.getLatestTestQueryEvent();
        assertEquals("2025-11-08 07:00:00.000", queryEvent.getRequestTime());
        assertEquals(ip, queryEvent.getAnswer());
        assertEquals("127.0.0.1", queryEvent.getClient());
        assertEquals("A", queryEvent.getType());
        assertEquals(domain + ".", queryEvent.getDomain());
        assertEquals("UPSTREAM", queryEvent.getQueryResolvedBy());

        Long elapsedTimeInMs = queryEvent.getElapsedTimeInMs();
        assertNotNull(elapsedTimeInMs);
        assertTrue(elapsedTimeInMs < 100);
    }

    private void fixHttpListenerUrls() {
        HttpListenerConf listeners = conf.getListeners();
        Set<String> http = listeners.getHttp();
        for (String url : http) {
            http.remove(url);
            http.add(httpListener.getUrl());
        }
        listeners.setHttp(http);
        conf.setListeners(listeners);
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
        httpListener.stop();
    }

}
