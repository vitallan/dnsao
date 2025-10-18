package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsSecModeConfTest {

    private ResolverConf getConf(String file) {
        InputStream input = getClass().getClassLoader().getResourceAsStream("dnssec/" + file);
        Conf conf = ConfLoader.load(input);
        return conf.getResolver();
    }

    @Test
    public void rigidMode() {
        ResolverConf resolverConf = getConf("rigid-mode.yml");
        assertEquals(DNSSecMode.RIGID, resolverConf.getDnsSecMode());
    }

    @Test
    public void simpleMode() {
        ResolverConf resolverConf = getConf("simple-mode.yml");
        assertEquals(DNSSecMode.SIMPLE, resolverConf.getDnsSecMode());
    }

    @Test
    public void offMode() {
        ResolverConf resolverConf = getConf("off-mode.yml");
        assertEquals(DNSSecMode.OFF, resolverConf.getDnsSecMode());
    }

    @Test
    public void invalidNameMode() {
        ResolverConf resolverConf = getConf("invalid-mode.yml");
        assertEquals(DNSSecMode.SIMPLE, resolverConf.getDnsSecMode());
    }

}
