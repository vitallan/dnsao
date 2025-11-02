package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.infra.clock.TimeProvider;
import com.allanvital.dnsao.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;
import static com.allanvital.dnsao.utils.TimeUtils.formatMillis;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestTimeProvider implements TimeProvider {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private AtomicReference<Long> now;

    public TestTimeProvider(long now) {
        this.now = new AtomicReference<>(now);
    }

    public void walkNow(int count, TimeUnit timeUnit) {
        walkNow(timeUnit.toMillis(count));
    }

    public void walkNow(long miliseconds) {
        Long newValue = now.get() + miliseconds;
        log.debug("now is: " + formatMillis(newValue, "HH:mm:ss.SSS"));
        now.set(newValue);
    }

    public void walkOneSecond() {
        walkNow(1100);
    }

    @Override
    public long currentTimeInMillis() {
        return now.get();
    }

}
