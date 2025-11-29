package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.bean.QuickResolver;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamPrioritizationTest extends TestHolder {

    private final QuickResolver quick1 = new QuickResolver();
    private final SlowResolver slow1 = new SlowResolver();
    private final SlowResolver slow2 = new SlowResolver();
    private final List<UpstreamResolver> resolvers = List.of(quick1, slow1, slow2);

    @BeforeEach
    public void setup() throws Exception {
        safeStart("3udp-upstream-nocache.yml");
        UpstreamResolverBuilder resolverBuilder = queryInfraAssembler.getResolverBuilder();
        resolverBuilder.setResolvers(resolvers);
    }

    @Test
    public void assertSimplePrioritizationOfBetterUpstream() throws Exception {
        for (int i = 1; i <= 10; i++) {
            executeRequestOnOwnServer("example.com");
            assertEquals(i, quick1.getCount());
        }
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
