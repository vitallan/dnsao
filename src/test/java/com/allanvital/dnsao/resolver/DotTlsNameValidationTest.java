package com.allanvital.dnsao.resolver;

import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.holder.DotTestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DotTlsNameValidationTest extends DotTestHolder {

    private List<UpstreamResolver> resolvers;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("dot/1dot-upstream-wrong-tls.yml");
        resolvers = queryInfraAssembler.getResolvers();
    }

    @Test
    public void shouldDiscardUpstreamWhenTlsNameValidationFails() {
        assertTrue(resolvers.isEmpty());
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

}
