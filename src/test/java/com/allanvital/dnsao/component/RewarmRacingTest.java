package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.TestTimeProvider;
import com.allanvital.dnsao.graph.bean.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.List;

import static com.allanvital.dnsao.infra.notification.EventType.CACHE_HIT;
import static com.allanvital.dnsao.infra.notification.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RewarmRacingTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        List<UpstreamResolver> resolvers = List.of(new QuickResolver(), new SlowResolver());
        TestResolverProvider testResolverProvider = new TestResolverProvider(resolvers);
        registerOverride(testResolverProvider);
        safeStart("1-udp-upstream-cache-multiplier.yml");
    }

    @Test
    public void duringRewarmTheLongerTtlResponseShouldBePrioritizedEvenWhenSlowerToRespond() throws Exception {
        executeRequestOnOwnServer(dnsServer, domain, false);
        Message cachedResponse = executeRequestOnOwnServer(dnsServer, domain, false);
        long cachedTtl = MessageHelper.extractTtlFromResponseMessage(cachedResponse);
        assertEquals(2, cachedTtl);

        eventListener.assertCount(CACHE_REWARM, 1);

        cachedResponse = executeRequestOnOwnServer(dnsServer, domain, false);
        eventListener.assertCount(CACHE_HIT, 2);
        cachedTtl = MessageHelper.extractTtlFromResponseMessage(cachedResponse);
        assertEquals(300, cachedTtl);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
