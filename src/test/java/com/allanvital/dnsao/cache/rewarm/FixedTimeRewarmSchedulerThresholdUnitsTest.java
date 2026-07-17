package com.allanvital.dnsao.cache.rewarm;

import com.allanvital.dnsao.graph.TestTimeProvider;
import com.allanvital.dnsao.infra.clock.Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FixedTimeRewarmSchedulerThresholdUnitsTest {

    @Test
    public void shouldScheduleTwentySecondsBeforeExpiryWhenThresholdIsConfiguredAsTwenty() {
        TestTimeProvider testTimeProvider = TestTimeProvider.getInstance();
        testTimeProvider.setNow(1_000_000L);
        Clock.setNewTimeProvider(testTimeProvider);

        FixedTimeRewarmScheduler scheduler = FixedTimeRewarmScheduler.fromSeconds(20);
        long expiryTime = testTimeProvider.currentTimeInMillis() + 60_000L;

        scheduler.schedule("A:example.com.", expiryTime);

        RewarmTask task = scheduler.queue().peek();
        assertNotNull(task);
        assertEquals(expiryTime - 20_000L, task.getTriggerAtMs());
    }
}
