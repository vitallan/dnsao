package com.allanvital.dnsao.infra.clock;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class Clock {

    private static TimeProvider timeProvider = new RealTimeProvider();

    public static long currentTimeInMillis() {
        return timeProvider.currentTimeInMillis();
    }

    public static void setNewTimeProvider(TimeProvider newTimeProvider) {
        timeProvider = newTimeProvider;
    }

}
