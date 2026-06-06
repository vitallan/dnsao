package com.allanvital.dnsao.graph;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.infra.clock.TimeProvider;

import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestTimeProvider implements TimeProvider {


    private long now;
    private final Object lock = new Object();

    private static TestTimeProvider INSTANCE;

    private TestTimeProvider() {}

    public static TestTimeProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestTimeProvider();
        }
        return INSTANCE;
    }

    public void setNow(long now) {
        synchronized (lock) {
            this.now = now;
            Log.INFRA.debug("now is: {}", formatMillis(now, "HH:mm:ss.SSS"));
        }
    }

    public void walkNow(int count, TimeUnit timeUnit) {
        walkNow(timeUnit.toMillis(count));
    }

    public void walkNow(long miliseconds) {
        synchronized (lock) {
            long newValue = now + miliseconds;
            Log.INFRA.debug("now is: " + formatMillis(newValue, "HH:mm:ss.SSS"));
            now = newValue;
        }
    }

    public void walkOneSecond() {
        walkNow(1300);
    }

    @Override
    public long currentTimeInMillis() {
        synchronized (lock) {
            return now;
        }
    }

}
