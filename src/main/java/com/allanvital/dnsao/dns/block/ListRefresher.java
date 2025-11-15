package com.allanvital.dnsao.dns.block;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ListRefresher implements Refresher {

    private static final int REFRESH_INTERVAL_IN_HOURS = 12;

    private final ScheduledExecutorService scheduler;

    public ListRefresher(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleRefresh(Runnable task) {
        scheduler.scheduleAtFixedRate(task, REFRESH_INTERVAL_IN_HOURS, REFRESH_INTERVAL_IN_HOURS, TimeUnit.HOURS);
    }

}
