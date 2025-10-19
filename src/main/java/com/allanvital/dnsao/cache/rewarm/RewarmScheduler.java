package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import com.allanvital.dnsao.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

import static com.allanvital.dnsao.AppLoggers.CACHE;
import static com.allanvital.dnsao.notification.EventType.REWARM_TASK_SCHEDULED;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmScheduler {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final NotificationManager notificationManager = NotificationManager.getInstance();

    private final DelayQueue<RewarmTask> queue = new DelayQueue<>();
    private final ConcurrentHashMap<String, RewarmTask> index = new ConcurrentHashMap<>();
    private final long thresholdToFire;

    public RewarmScheduler(long thresholdToFire) {
        this.thresholdToFire = thresholdToFire;
    }

    public void schedule(String key, long entryTtl) {
        long now = System.currentTimeMillis();
        long triggerAt = Math.max(now, entryTtl - thresholdToFire);
        RewarmTask fresh = new RewarmTask(key, triggerAt);
        log.debug("scheduling {} to rewarm at {}", key, TimeUtils.formatMillis(triggerAt, "HH:mm:ss"));
        RewarmTask old = index.put(key, fresh);
        if (old != null) {
            queue.remove(old);
        }
        notificationManager.notify(REWARM_TASK_SCHEDULED);
        queue.put(fresh);
    }

    public void cancel(String key) {
        RewarmTask old = index.remove(key);
        log.debug("canceling scheduling of {}", key);
        if (old != null) {
            queue.remove(old);
        }
    }

    public DelayQueue<RewarmTask> queue() { return queue; }

    public ConcurrentHashMap<String, RewarmTask> index() { return index; }

}
