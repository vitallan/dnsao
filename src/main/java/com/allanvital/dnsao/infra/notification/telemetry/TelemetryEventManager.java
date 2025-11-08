package com.allanvital.dnsao.infra.notification.telemetry;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TelemetryEventManager {

    private static volatile TelemetryEventManager INSTANCE;
    private static boolean isTelemetryEnabled = false;
    private final List<EventListener> listeners = new LinkedList<>();

    public static TelemetryEventManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TelemetryEventManager();
        }
        return INSTANCE;
    }

    public static void enableTelemetry(boolean enabled) {
        isTelemetryEnabled = enabled;
    }

    public static void telemetryNotify(EventType event) {
        if (!isTelemetryEnabled) {
            return;
        }
        getInstance().notifyEvent(event);
    }

    public static void telemetrySubscribe(EventListener listener) {
        getInstance().subscribe(listener);
    }

    public static void telemetryUnsubscribe(EventListener listener) {
        getInstance().unsubscribe(listener);
    }

    public void notifyEvent(EventType event) {
        for (EventListener listener : listeners) {
            listener.receiveNotification(event);
        }
    }

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

}
