package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.graph.bean.FakeResolverWithBarrier;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MultiplierTest extends TestHolder {

    private String domain = "example.com";
    private AtomicInteger counter;
    private CyclicBarrier barrier;
    private List<UpstreamResolver> resolvers;
    private QueryProcessor processor;

    @BeforeEach
    public void setup() throws Exception {
        counter = new AtomicInteger(0);
        barrier = new CyclicBarrier(3);
        resolvers = List.of(
                new FakeResolverWithBarrier(barrier, true, counter),
                new FakeResolverWithBarrier(barrier, false, counter),
                new FakeResolverWithBarrier(barrier, false, counter)
        );
        TestResolverProvider testResolverProvider = new TestResolverProvider(resolvers);
        registerOverride(testResolverProvider);
        safeStart("1udp-upstream-3-multiplier.yml");
        QueryProcessorFactory factory = assembler.getQueryProcessorFactory();
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void singleQueryShouldResultInMultiplierNumberOfUpstreamRequests() throws Exception {
        Message request = MessageHelper.buildARequest(domain);
        processor.processQuery(getClient(), request.toWire());
        Assertions.assertEquals(3, counter.get());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
