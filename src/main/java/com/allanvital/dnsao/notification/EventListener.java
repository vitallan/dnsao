package com.allanvital.dnsao.notification;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface EventListener {
// this is currently used only for testing. i should think of a way to centralize the notification/notificationTypes
    void receiveNotification(EventType type);

}
