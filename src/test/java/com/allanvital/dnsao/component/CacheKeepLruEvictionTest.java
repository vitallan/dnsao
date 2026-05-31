package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CacheKeepLruEvictionTest extends TestHolder {

    private static final String KEEP1 = "keep1.com";
    private static final String KEEP2 = "keep2.com";
    private static final String NON_KEEP = "nonkeep.com";

    @BeforeEach
    public void setup() throws Exception {
        loadConf("cache/1udp-cache-keep-lru-max1.yml");
        conf.getMisc().setQueryLog(false);

        startFakeServer();
        prepareSimpleMockResponse(KEEP1, "10.10.10.10", 10_000);
        prepareSimpleMockResponse(KEEP2, "10.10.10.20", 10_000);
        prepareSimpleMockResponse(NON_KEEP, "10.10.10.30", 10_000);

        safeStartWithPresetConf(true);

        executeRequestOnOwnServer(KEEP1);
        executeRequestOnOwnServer(KEEP2);

        fakeUpstreamServer.clearCallCount();
        eventListener.reset();
        assertEquals(0, fakeUpstreamServer.getCallCount());
    }

    @Test
    public void keepEntryShouldNotBeEvictedWhenNonKeepIsInsertedUnderLruPressure() throws IOException {
        executeRequestOnOwnServer(KEEP1);
        assertEquals(0, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(NON_KEEP);
        assertEquals(1, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(KEEP1);
        assertEquals(1, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(NON_KEEP);
        assertEquals(2, fakeUpstreamServer.getCallCount());
    }

    @Test
    public void whenAllEntriesAreKeepCacheMayExceedMaxSizeAndBothRemainCached() throws IOException {
        executeRequestOnOwnServer(KEEP1);
        assertEquals(0, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(KEEP2);
        assertEquals(0, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(KEEP1);
        assertEquals(0, fakeUpstreamServer.getCallCount());

        executeRequestOnOwnServer(KEEP2);
        assertEquals(0, fakeUpstreamServer.getCallCount());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
