package com.allanvital.dnsao;

import com.allanvital.dnsao.notification.EventListener;
import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestEventListener implements EventListener {

    Map<EventType, Integer> eventCounts = new HashMap<>();

    public TestEventListener() {
        NotificationManager.getInstance().subscribe(this);
    }

    @Override
    public void receiveNotification(EventType type) {
        if (eventCounts.containsKey(type)) {
            eventCounts.put(type, eventCounts.get(type) + 1);
            return;
        }
        eventCounts.put(type, 1);
    }

    private int get(EventType type) {
        Integer count = eventCounts.get(type);
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }

    public void reset() {
        eventCounts = new HashMap<>();
    }

    public void assertCount(EventType eventType, int expectedCount) {
        Assertions.assertEquals(expectedCount, get(eventType));
    }

    public void waitEventCount(EventType type, int countToWaitTo) throws InterruptedException {
        Integer count = get(type);
        int maxSleepCount = 100; //max 10 seconds
        int sleepCount = 0;
        while (count != countToWaitTo) {
            Thread.sleep(100);
            sleepCount++;
            count = get(type);
            if (sleepCount == maxSleepCount) {
                break;
            }
        }
        Assertions.assertEquals(countToWaitTo, count, "the event count for " + type + " never reached " + countToWaitTo + ". count was " + count );
    }

}