package com.allanvital.dnsao.cache;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.SCAVENGER_RAN;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheScavenger implements Runnable {

    private final CacheManager cacheManager;

    public CacheScavenger(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void run() {
        cacheManager.purgeExpired();
        telemetryNotify(SCAVENGER_RAN);
    }

}
