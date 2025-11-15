package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.infra.notification.telemetry.EventListener;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetrySubscribe;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryUnsubscribe;

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
    public synchronized void receiveNotification(EventType type) {
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
        return count;
    }

    public void reset() {
        telemetryUnsubscribe(this);
        eventCounts = new HashMap<>();
        telemetrySubscribe(this);
    }

    public void assertCount(EventType type, int countToWaitTo) throws InterruptedException {
        assertCount(type, countToWaitTo, true);
    }

    public void assertCount(EventType type, int countToWaitTo, boolean withTimeWalk) throws InterruptedException {
        Integer count = get(type);
        int maxSleepCount = 200;
        int sleepCount = 0;
        while (count != countToWaitTo) {
            if (countToWaitTo < count) {
                Assertions.fail("the expected count is already gone. Expected: " + countToWaitTo + " Got: " + count );
            }
            if (withTimeWalk) {
                testTimeProvider.walkOneSecond();
            }
            Thread.sleep(30);
            sleepCount++;
            count = get(type);
            if (sleepCount == maxSleepCount) {
                break;
            }
        }
        Assertions.assertEquals(countToWaitTo, count, "the event count for " + type + " never reached " + countToWaitTo + ". count was " + count );
    }

}