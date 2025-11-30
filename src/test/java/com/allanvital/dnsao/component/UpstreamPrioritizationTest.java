package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.UpstreamResolverBuilder;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.ResolverProvider;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.QuickResolver;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.UPSTREAM_PRIORITIZED;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamPrioritizationTest extends TestHolder {

    private final QuickResolver quick1 = new QuickResolver();
    private final SlowResolver slow1 = new SlowResolver();
    private final SlowResolver slow2 = new SlowResolver();
    private final List<UpstreamResolver> resolvers = List.of(quick1, slow1, slow2);
    private QueryProcessor queryProcessor;
    private ResolverProvider resolverProvider;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("3udp-upstream-nocache.yml");
        UpstreamResolverBuilder resolverBuilder = queryInfraAssembler.getResolverBuilder();
        resolverBuilder.setResolvers(resolvers);
        resolverProvider = queryInfraAssembler.getResolverProvider();
        QueryProcessorFactory queryProcessorFactory = assembler.getQueryProcessorFactory();
        queryProcessor = queryProcessorFactory.buildQueryProcessor();
    }

    @Test
    public void assertSimplePrioritizationOfBetterUpstream() throws Exception {
        for (int i = 1; i <= 10; i++) {
            executeRequestOnOwnServer("example.com");
            assertEquals(i, quick1.getCount());
        }
    }

    @Test
    public void assertPrioritizeOnlyOnExternalQueries() throws Exception {
        String domain = "example.com";
        executeRequestOnOwnServer(domain);
        queryProcessor.processInternalQuery(MessageHelper.buildARequest(domain));
        queryProcessor.processInternalQuery(MessageHelper.buildARequest(domain));
        eventListener.assertCount(UPSTREAM_PRIORITIZED, 1, false);
        assertResolverOnList(quick1);
    }

    private void assertResolverOnList(UpstreamResolver resolver) {
        assertTrue(resolverProvider.getResolversToUse().contains(resolver));
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
