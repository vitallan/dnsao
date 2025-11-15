package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.*;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DefaultConfTest extends TestHolder {

    @BeforeEach
    public void setup() throws ConfException {
        loadConf("empty.yml", false);
    }

    @Test
    public void validateEmptyServerConfiguration() {
        ServerConf serverConf = conf.getServer();
        assertEquals(1053, serverConf.getPort());
        assertEquals(8044, serverConf.getWebPort());
        assertEquals(20, serverConf.getUdpThreadPool());
        assertEquals(4, serverConf.getTcpThreadPool());
    }

    @Test
    public void validateEmptyCacheConfiguration() {
        CacheConf cache = conf.getCache();
        assertFalse(cache.isEnabled());
        assertFalse(cache.isRewarm());
        assertEquals(10000, cache.getMaxCacheEntries());
        assertEquals(3, cache.getMaxRewarmCount());
    }

    @Test
    public void validateEmptyResolver() {
        ResolverConf resolverConf = conf.getResolver();
        List<Upstream> resolverConfUpstreams = resolverConf.getUpstreams();
        assertEquals(1, resolverConf.getMultiplier());
        assertEquals(3, resolverConf.getTlsPoolSize());

        assertEquals(DNSSecMode.SIMPLE, conf.getMisc().getDnsSecMode());
        assertTrue(resolverConf.getLocalMappings().isEmpty());

        List<Upstream> upstreams = resolverConf.getUpstreams();
        assertEquals(resolverConfUpstreams, upstreams);

        assertFalse(upstreams.isEmpty());
        assertEquals(1, upstreams.size());

        Upstream upstream = upstreams.get(0);
        assertEquals("9.9.9.9", upstream.getIp());
        assertEquals("udp", upstream.getProtocol());
        assertEquals(53, upstream.getPort());
        assertNull(upstream.getTlsAuthName());
    }

}
