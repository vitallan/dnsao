package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.SlowResolver;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ResolverTimeoutTest extends TestHolder {

    private String domain = "example.com";

    @BeforeEach
    public void setup() throws ConfException {
        List<UpstreamResolver> resolvers = List.of(new SlowResolver(1100));
        TestResolverProvider testResolverProvider = new TestResolverProvider(resolvers);
        registerOverride(testResolverProvider);
        safeStart("1-udp-upstream-cache-multiplier.yml");
    }

    @Test
    public void shouldEnforceTimeoutConfig() throws IOException {
        Message message = executeRequestOnOwnServer(dnsServer, domain, false);
        assertEquals(Rcode.SERVFAIL, message.getRcode());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

}
