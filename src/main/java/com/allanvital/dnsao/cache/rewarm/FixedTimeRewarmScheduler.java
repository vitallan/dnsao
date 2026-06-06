package com.allanvital.dnsao.cache.rewarm;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.utils.TimeUtils;

import java.util.concurrent.DelayQueue;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.REWARM_TASK_SCHEDULED;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FixedTimeRewarmScheduler {


    private final DelayQueue<RewarmTask> queue = new DelayQueue<>();
    private final long thresholdToFire;

    public FixedTimeRewarmScheduler(long thresholdToFire) {
        this.thresholdToFire = thresholdToFire;
    }

    public void schedule(String key, long entryTtl) {
        long now = Clock.currentTimeInMillis();
        long triggerAt = entryTtl - thresholdToFire;
        if (triggerAt <= now) {
            Log.CACHE.debug("the ttl entry for key {} is immediate. Ignoring entry ", key);
            remove(key);
            return;
        }
        RewarmTask task = new RewarmTask(key, triggerAt);
        Log.CACHE.debug("scheduling {} to rewarm at {}", key, TimeUtils.formatMillis(triggerAt, "HH:mm:ss"));
        remove(key);
        telemetryNotify(REWARM_TASK_SCHEDULED);
        queue.put(task);
    }

    public void cancel(String key) {
        remove(key);
        Log.CACHE.debug("canceling scheduling of {}", key);
    }

    private void remove(String key) {
        RewarmTask task = new RewarmTask(key, Clock.currentTimeInMillis());
        queue.remove(task);
    }

    public DelayQueue<RewarmTask> queue() { return queue; }

}
