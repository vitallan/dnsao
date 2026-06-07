package com.allanvital.dnsao.infra.log;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class AsyncConsoleHandler extends AsyncLogHandler {

    private final SystemOutHandler delegate;

    public AsyncConsoleHandler(Formatter formatter) {
        this.delegate = new SystemOutHandler(formatter);
        this.delegate.setLevel(Level.ALL);
        startWorker("dnsao-console-worker");
    }

    @Override
    protected void publishRecord(LogRecord record) {
        delegate.publish(record);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

}
