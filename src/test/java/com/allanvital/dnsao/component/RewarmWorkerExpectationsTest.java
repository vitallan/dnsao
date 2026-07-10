package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.BlockingResolver;
import com.allanvital.dnsao.graph.bean.FlakyResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RewarmWorkerExpectationsTest extends TestHolder {

    private static final String DOMAIN = "rewarm.example.com";
    private static final String KEY = "A:" + DOMAIN;

    private CacheManager cacheManager;

    @AfterEach
    void tearDown() throws InterruptedException {
        safeStop();
    }

    @Test
    void staleRewarmTaskShouldNotOverwriteANewerCacheEntry() throws Exception {
        BlockingResolver resolver = new BlockingResolver("10.10.10.10", 300);
        startWithResolver(resolver);

        Message initial = MessageHelper.buildAResponse(MessageHelper.buildARequest(DOMAIN), "1.1.1.1", 1);
        Message newer = MessageHelper.buildAResponse(MessageHelper.buildARequest(DOMAIN), "20.20.20.20", 30);

        cacheManager.put(KEY, initial, 1L);
        testTimeProvider.walkNow(1200);
        assertTrue(resolver.started.await(1, TimeUnit.SECONDS), "rewarm should have started");

        cacheManager.put(KEY, newer, 30L);
        resolver.proceed.countDown();

        assertEventually(() -> {
            Message current = cacheManager.safeGet(KEY).getResponse();
            return current != null && "20.20.20.20".equals(MessageHelper.extractIpFromResponseMessage(current));
        }, "newer cache entry should remain after obsolete rewarm task completes");
        Message finalResponse = cacheManager.safeGet(KEY).getResponse();
        assertNotNull(finalResponse);
        assertEquals("20.20.20.20", MessageHelper.extractIpFromResponseMessage(finalResponse));
    }

    @Test
    void transientRewarmFailureShouldBeRetried() throws Exception {
        FlakyResolver resolver = new FlakyResolver("30.30.30.30", 300);
        startWithResolver(resolver);

        Message initial = MessageHelper.buildAResponse(MessageHelper.buildARequest(DOMAIN), "1.1.1.1", 1);

        cacheManager.put(KEY, initial, 1L);
        testTimeProvider.walkNow(1200);
        assertTrue(resolver.firstAttempt.await(1, TimeUnit.SECONDS), "first rewarm attempt should happen");

        testTimeProvider.walkNow(1200);
        assertTrue(resolver.secondAttempt.await(1, TimeUnit.SECONDS), "rewarm should retry after a transient failure");
        eventListener.assertCount(CACHE_REWARM, 1, false);
    }

    private void startWithResolver(UpstreamResolver resolver) throws ConfException {
        registerOverride(new FixedTimeRewarmScheduler(100));
        registerOverride(new TestResolverProvider(List.of(resolver)));
        safeStart("1udp-upstream-cache-rewarm.yml");
        cacheManager = assembler.getCacheManager();
    }

    private void assertEventually(BooleanSupplier condition, String failureMessage) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), failureMessage);
    }
}
