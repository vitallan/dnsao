package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.BrokenResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmBrokenUpstreamTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        List<UpstreamResolver> resolvers = List.of(new BrokenResolver(), new SlowResolver());
        TestResolverProvider testResolverProvider = new TestResolverProvider(resolvers);
        registerOverride(testResolverProvider);
        safeStart("1-udp-upstream-cache-multiplier.yml");
    }

    @Test
    public void duringRewarmEvenWhenAResolverFailsItShouldNotBreakTheRewarmingProcess() throws Exception {
        executeRequestOnOwnServer(dnsServer, domain, false);
        Message cachedResponse = executeRequestOnOwnServer(dnsServer, domain, false);
        long cachedTtl = MessageHelper.extractTtlFromResponseMessage(cachedResponse);
        assertEquals(300, cachedTtl);

        testTimeProvider.walkNow(301, TimeUnit.SECONDS);
        eventListener.assertCount(CACHE_REWARM, 1, false);

        cachedResponse = executeRequestOnOwnServer(dnsServer, domain, false);
        cachedTtl = MessageHelper.extractTtlFromResponseMessage(cachedResponse);
        assertEquals(300, cachedTtl);
        eventListener.assertCount(CACHE_HIT, 2, false);
        eventListener.assertCount(QUERY_RESOLVED, 4, false); //3 requests, 1 rewarm
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
