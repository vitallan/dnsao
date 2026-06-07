package com.allanvital.dnsao.infra.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncConsoleHandlerTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Test
    void shouldOutputPublishedRecordToSystemOut() {
        System.setOut(new PrintStream(out));
        AsyncConsoleHandler handler = new AsyncConsoleHandler(new LogFormatter());

        NamedLogRecord record = new NamedLogRecord(Level.INFO, "test message");
        record.setLoggerName("TEST");
        handler.publish(record);
        handler.close();

        String output = out.toString();
        assertTrue(output.contains("test message"), "should contain the log message");
    }

    @Test
    void shouldNotOutputWhenNotLoggable() {
        System.setOut(new PrintStream(out));
        AsyncConsoleHandler handler = new AsyncConsoleHandler(new LogFormatter());
        handler.setLevel(Level.OFF);

        NamedLogRecord record = new NamedLogRecord(Level.INFO, "should not appear");
        record.setLoggerName("TEST");
        handler.publish(record);
        handler.close();

        String output = out.toString();
        assertTrue(output.isEmpty(), "should not output when handler level is OFF");
    }

    @Test
    void shouldOutputMultipleRecordsInOrder() {
        System.setOut(new PrintStream(out));
        AsyncConsoleHandler handler = new AsyncConsoleHandler(new LogFormatter());

        NamedLogRecord first = new NamedLogRecord(Level.INFO, "first message");
        first.setLoggerName("TEST");
        NamedLogRecord second = new NamedLogRecord(Level.WARNING, "second message");
        second.setLoggerName("TEST");
        handler.publish(first);
        handler.publish(second);
        handler.close();

        String output = out.toString();
        int firstIndex = output.indexOf("first message");
        int secondIndex = output.indexOf("second message");
        assertTrue(firstIndex >= 0, "should contain first message");
        assertTrue(secondIndex >= 0, "should contain second message");
        assertTrue(firstIndex < secondIndex, "first message should appear before second");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

}
