package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;
import static com.allanvital.dnsao.infra.notification.EventType.REWARM_TASK_SCHEDULED;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FixedTimeRewarmScheduler {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final NotificationManager notificationManager = NotificationManager.getInstance();

    private final DelayQueue<RewarmTask> queue = new DelayQueue<>();
    private final long thresholdToFire;

    public FixedTimeRewarmScheduler(long thresholdToFire) {
        this.thresholdToFire = thresholdToFire;
    }

    public void schedule(String key, long entryTtl) {
        long now = Clock.currentTimeInMillis();
        long triggerAt = Math.max(now, entryTtl - thresholdToFire);
        RewarmTask task = new RewarmTask(key, triggerAt);
        log.debug("scheduling {} to rewarm at {}", key, TimeUtils.formatMillis(triggerAt, "HH:mm:ss"));
        remove(key);
        notificationManager.notify(REWARM_TASK_SCHEDULED);
        queue.put(task);
    }

    public void cancel(String key) {
        remove(key);
        log.debug("canceling scheduling of {}", key);
    }

    private void remove(String key) {
        RewarmTask task = new RewarmTask(key, Clock.currentTimeInMillis());
        queue.remove(task);
    }

    public DelayQueue<RewarmTask> queue() { return queue; }

}
