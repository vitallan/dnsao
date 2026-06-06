package com.allanvital.dnsao.infra.log;

import com.allanvital.dnsao.conf.inner.LogConf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTest {

    private final List<LogEvent> captured = new ArrayList<>();

    record LogEvent(Level level, String category, String message) {}

    @BeforeEach
    void setUp() {
        Log.setHandler((level, category, message) -> captured.add(new LogEvent(level, category, message)));
        LogConfigurator.reset();
        LogConf conf = new LogConf();
        conf.setDns("TRACE");
        conf.setCache("TRACE");
        conf.setInfra("TRACE");
        conf.setRootLevel("TRACE");
        LogConfigurator.configure(conf);
    }

    @AfterEach
    void tearDown() {
        LogConfigurator.reset();
        Log.setHandler(null);
    }

    @Test
    void shouldCaptureDnsInfoMessage() {
        Log.DNS.info("DNS server stopped");
        assertEquals(1, captured.size());
        LogEvent event = captured.get(0);
        assertEquals(Level.INFO, event.level());
        assertEquals("DNS", event.category());
        assertEquals("DNS server stopped", event.message());
    }

    @Test
    void shouldCaptureCacheInfoMessage() {
        Log.CACHE.info("cache cleared");
        assertEquals(1, captured.size());
        LogEvent event = captured.get(0);
        assertEquals(Level.INFO, event.level());
        assertEquals("CACHE", event.category());
        assertEquals("cache cleared", event.message());
    }

    @Test
    void shouldCaptureInfraInfoMessage() {
        Log.INFRA.info("server started");
        assertEquals(1, captured.size());
        LogEvent event = captured.get(0);
        assertEquals(Level.INFO, event.level());
        assertEquals("INFRA", event.category());
        assertEquals("server started", event.message());
    }

    @Test
    void shouldCaptureTraceLevel() {
        Log.DNS.trace("detailed trace");
        assertEquals(1, captured.size());
        assertEquals(Level.FINER, captured.get(0).level());
    }

    @Test
    void shouldCaptureDebugLevel() {
        Log.DNS.debug("debug value");
        assertEquals(1, captured.size());
        assertEquals(Level.FINE, captured.get(0).level());
    }

    @Test
    void shouldCaptureWarnLevel() {
        Log.DNS.warn("warning message");
        assertEquals(1, captured.size());
        assertEquals(Level.WARNING, captured.get(0).level());
    }

    @Test
    void shouldCaptureErrorLevel() {
        Log.DNS.error("error occurred");
        assertEquals(1, captured.size());
        assertEquals(Level.SEVERE, captured.get(0).level());
    }

    @Test
    void shouldFormatParametersInMessage() {
        Log.DNS.info("value is {} and {}", "hello", 42);
        assertEquals(1, captured.size());
        assertEquals("value is hello and 42", captured.get(0).message());
    }

    @Test
    void shouldNotAddGroupSeparatorToNumbers() {
        Log.DNS.info("Started {} on port {}", "udp-server", 1053);
        assertEquals(1, captured.size());
        assertEquals("Started udp-server on port 1053", captured.get(0).message());
    }

    @Test
    void shouldHandleSingleQuoteInMessage() {
        Log.DNS.info("it's working");
        assertEquals(1, captured.size());
        assertEquals("it's working", captured.get(0).message());
    }

    @Test
    void shouldIncludeThrowableInMessageWhenDebugEnabled() {
        RuntimeException ex = new RuntimeException("test failure");
        Log.DNS.error("operation failed: {}", "details", ex);
        assertEquals(1, captured.size());
        assertTrue(captured.get(0).message().contains("operation failed: details"),
            "message should contain formatted text");
    }

    @Test
    void shouldNotLogWhenLevelIsOff() {
        LogConfigurator.reset();
        LogConf conf = new LogConf();
        conf.setDns("OFF");
        LogConfigurator.configure(conf);

        Log.DNS.info("should not appear");
        assertEquals(0, captured.size(), "should not capture when level is OFF");
    }

    @Test
    void shouldLogWithEscapedQuotesAndPlaceholders() {
        Log.DNS.warn("Error on key '{}' rewarmWorker: {}", "example.com", "NPE");
        assertEquals(1, captured.size());
        assertEquals("Error on key 'example.com' rewarmWorker: NPE", captured.get(0).message());
    }

}
