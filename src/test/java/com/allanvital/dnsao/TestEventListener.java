package com.allanvital.dnsao;

import com.allanvital.dnsao.graph.TestTimeProvider;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.EventListener;
import com.allanvital.dnsao.infra.notification.EventType;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestEventListener implements EventListener {

    private TestTimeProvider testTimeProvider;
    private Map<EventType, Integer> eventCounts = new HashMap<>();

    public TestEventListener(TestTimeProvider testTimeProvider) {
        reset();
        this.testTimeProvider = testTimeProvider;
    }

    @Override
    public void receiveNotification(EventType type) {
        synchronized (this) {
            if (eventCounts.containsKey(type)) {
                eventCounts.put(type, eventCounts.get(type) + 1);
                return;
            }
            eventCounts.put(type, 1);
        }
    }

    private int get(EventType type) {
        Integer count = eventCounts.get(type);
        if (count == null) {
            return 0;
        }
        return count.intValue();
    }

    public void reset() {
        NotificationManager.getInstance().unsubscribe(this);
        eventCounts = new HashMap<>();
        NotificationManager.getInstance().subscribe(this);
    }

    public void assertCount(EventType type, int countToWaitTo) throws InterruptedException {
        Integer count = get(type);
        int maxSleepCount = 100;
        int sleepCount = 0;
        while (count != countToWaitTo) {
            if (countToWaitTo < count) {
                Assertions.fail("the expected count is already gone. Expected: " + countToWaitTo + " Got: " + count );
            }
            testTimeProvider.walkOneSecond();
            Thread.sleep(60);
            sleepCount++;
            count = get(type);
            if (sleepCount == maxSleepCount) {
                break;
            }
        }
        Assertions.assertEquals(countToWaitTo, count, "the event count for " + type + " never reached " + countToWaitTo + ". count was " + count );
    }

}