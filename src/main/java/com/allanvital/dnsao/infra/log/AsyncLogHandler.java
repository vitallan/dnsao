package com.allanvital.dnsao.infra.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AsyncLogHandler extends Handler {

    protected static final long POLL_TIMEOUT_SECONDS = 1;

    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread worker;

    protected void startWorker(String threadName) {
        this.worker = new Thread(this::processQueue, threadName);
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
                    publishRecord(record);
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
            publishRecord(record);
        }
    }

    protected abstract void publishRecord(LogRecord record);

    @Override
    public void close() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        drainQueue();
        closeResources();
    }

    protected void closeResources() {
    }

}
