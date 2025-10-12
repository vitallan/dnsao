package com.allanvital.dnsao.utils;

import com.allanvital.dnsao.dns.remote.DnsUtils;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockedTest {

    private Set<String> blockedSet = new HashSet<>();

    @Test
    public void shouldCorrectlyBlockSubdomain() throws TextParseException {
        blockedSet.add("betano.com");
        assertTrue(DnsUtils.isBlocked(Name.fromString("betano.com."), blockedSet));
        assertTrue(DnsUtils.isBlocked(Name.fromString("www.betano.com."), blockedSet));
        assertFalse(DnsUtils.isBlocked(Name.fromString("soubetano.com."), blockedSet));
    }

}
