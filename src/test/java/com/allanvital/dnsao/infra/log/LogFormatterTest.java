package com.allanvital.dnsao.infra.log;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogFormatterTest {

    private final LogFormatter formatter = new LogFormatter();

    @Test
    void shouldFormatInfoLine() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "test message");
        record.setLoggerName("DNS");

        String result = formatter.format(record);

        assertTrue(result.matches(
            "\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}] INFO  \\[.+?] \\[DNS] test message\\r?\\n?"
        ), "unexpected format: " + result);
    }

    @Test
    void shouldMapSevereToError() {
        NamedLogRecord record = new NamedLogRecord(Level.SEVERE, "fail");
        record.setLoggerName("INFRA");

        String result = formatter.format(record);

        assertTrue(result.contains("ERROR"), "expected ERROR level");
    }

    @Test
    void shouldMapWarningToWarn() {
        NamedLogRecord record = new NamedLogRecord(Level.WARNING, "caution");
        record.setLoggerName("CACHE");

        String result = formatter.format(record);

        assertTrue(result.contains("WARN "), "expected WARN level with padding");
    }

    @Test
    void shouldMapFineToDebug() {
        NamedLogRecord record = new NamedLogRecord(Level.FINE, "debug info");
        record.setLoggerName("DNS");

        String result = formatter.format(record);

        assertTrue(result.contains("DEBUG"), "expected DEBUG level");
    }

    @Test
    void shouldMapFinerToTrace() {
        NamedLogRecord record = new NamedLogRecord(Level.FINER, "trace info");
        record.setLoggerName("DNS");

        String result = formatter.format(record);

        assertTrue(result.contains("TRACE"), "expected TRACE level");
    }

    @Test
    void shouldIncludeThreadNameFromNamedLogRecord() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "msg");
        record.setLoggerName("DNS");

        String result = formatter.format(record);

        assertTrue(result.contains("[" + record.getThreadName() + "]"),
            "expected thread name " + record.getThreadName());
    }

    @Test
    void shouldFormatMessageWithParameters() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "value is {0}");
        record.setLoggerName("DNS");
        record.setParameters(new Object[]{42});

        String result = formatter.format(record);

        assertTrue(result.contains("value is 42"), "unexpected: " + result);
    }

    @Test
    void shouldNotAddGroupSeparatorToNumbers() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "Started {0} on port {1}");
        record.setLoggerName("DNS");
        record.setParameters(new Object[]{"udp-server", 1053});

        String result = formatter.format(record);

        assertFalse(result.contains("1,053"),
            "should not contain group separator: " + result);
        assertTrue(result.contains("port 1053"),
            "expected port without separator: " + result);
    }

    @Test
    void shouldIncludeThrowableWhenPresent() {
        NamedLogRecord record = new NamedLogRecord(Level.WARNING, "something broke");
        record.setLoggerName("INFRA");
        record.setThrown(new RuntimeException("test exception"));

        String result = formatter.format(record);

        assertTrue(result.contains("java.lang.RuntimeException"), "expected exception class");
        assertTrue(result.contains("test exception"), "expected exception message");
    }

    @Test
    void shouldNotIncludeThrowableWhenAbsent() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "all good");
        record.setLoggerName("DNS");

        String result = formatter.format(record);

        assertFalse(result.contains("Exception"), "unexpected stack trace");
        assertTrue(result.contains("all good"), "expected message");
    }

    @Test
    void shouldFormatCacheCategory() {
        NamedLogRecord record = new NamedLogRecord(Level.WARNING, "cache miss");
        record.setLoggerName("CACHE");

        String result = formatter.format(record);

        assertTrue(result.contains("[CACHE]"), "expected CACHE category");
    }

    @Test
    void shouldFormatInfraCategory() {
        NamedLogRecord record = new NamedLogRecord(Level.INFO, "server started");
        record.setLoggerName("INFRA");

        String result = formatter.format(record);

        assertTrue(result.contains("[INFRA]"), "expected INFRA category");
    }

}
