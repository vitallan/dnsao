package com.allanvital.dnsao.infra.log;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class AsyncFileHandler extends AsyncLogHandler {

    private final java.util.logging.FileHandler delegate;

    public AsyncFileHandler(String pattern, long maxSize, int maxFiles, boolean append) throws IOException {
        int limit = (int) Math.min(maxSize, Integer.MAX_VALUE);
        this.delegate = new java.util.logging.FileHandler(pattern, limit, maxFiles, append);
        this.delegate.setFormatter(new LogFormatter());
        this.delegate.setLevel(Level.ALL);
        startWorker("dnsao-log-worker");
    }

    @Override
    protected void publishRecord(LogRecord record) {
        delegate.publish(record);
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    protected void closeResources() {
        delegate.close();
    }

}
