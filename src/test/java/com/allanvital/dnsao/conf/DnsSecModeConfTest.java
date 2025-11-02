package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsSecModeConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "dnssec";
    }

    @Test
    public void rigidMode() {
        assertEquals(DNSSecMode.RIGID, getConf("rigid-mode.yml").getMisc().getDnsSecMode());
    }

    @Test
    public void simpleMode() {
        assertEquals(DNSSecMode.SIMPLE, getConf("simple-mode.yml").getMisc().getDnsSecMode());
    }

    @Test
    public void offMode() {
        assertEquals(DNSSecMode.OFF, getConf("off-mode.yml").getMisc().getDnsSecMode());
    }

    @Test
    public void invalidNameMode() {
        assertEquals(DNSSecMode.SIMPLE, getConf("invalid-mode.yml").getMisc().getDnsSecMode());
    }

}
