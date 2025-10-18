package com.allanvital.dnsao.component;

import com.allanvital.dnsao.TestHolder;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.QueryProcessor;
import com.allanvital.dnsao.dns.remote.QueryProcessorFactory;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import com.allanvital.dnsao.helper.FakeResolverWithBarrier;
import com.allanvital.dnsao.helper.MessageUtils;
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
    private List<NamedResolver> resolvers;
    private QueryProcessor processor;

    @BeforeEach
    public void setup() throws Exception {
        super.loadConf("1udp-upstream-3-multiplier.yml", false);
        int multiplier = conf.getResolver().getMultiplier();
        counter = new AtomicInteger(0);
        barrier = new CyclicBarrier(multiplier);
        resolvers = List.of(
                new FakeResolverWithBarrier(barrier, true, counter),
                new FakeResolverWithBarrier(barrier, false, counter),
                new FakeResolverWithBarrier(barrier, false, counter)
        );
        QueryProcessorFactory factory = new QueryProcessorFactory(resolvers, null, null, null, multiplier, DNSSecMode.OFF);
        processor = factory.buildQueryProcessor();
    }

    @Test
    public void singleQueryShouldResultInMultiplierNumberOfUpstreamRequests() throws Exception {
        Message request = MessageUtils.buildARequest(domain);
        processor.processQuery(getClient(), request.toWire());
        Assertions.assertEquals(3, counter.get());
    }

}
