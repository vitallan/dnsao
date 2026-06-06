package com.allanvital.dnsao.infra.log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class AsyncFileHandler extends Handler {

    private static final long POLL_TIMEOUT_SECONDS = 1;

    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>();
    private final java.util.logging.FileHandler delegate;
    private final Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public AsyncFileHandler(String pattern, long maxSize, int maxFiles, boolean append) throws IOException {
        int limit = (int) Math.min(maxSize, Integer.MAX_VALUE);
        this.delegate = new java.util.logging.FileHandler(pattern, limit, maxFiles, append);
        this.delegate.setFormatter(new LogFormatter());
        this.delegate.setLevel(Level.ALL);
        this.worker = new Thread(this::processQueue, "dnsao-log-worker");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        queue.offer(record);
    }

    private void processQueue() {
        while (running.get()) {
            try {
                LogRecord record = queue.poll(POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (record != null) {
                    delegate.publish(record);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        drainQueue();
    }

    private void drainQueue() {
        List<LogRecord> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        for (LogRecord record : remaining) {
            delegate.publish(record);
        }
    }

    @Override
    public void flush() {
        delegate.flush();
    }

    @Override
    public void close() {
        running.set(false);
        worker.interrupt();
        try {
            worker.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        drainQueue();
        delegate.close();
    }

}
