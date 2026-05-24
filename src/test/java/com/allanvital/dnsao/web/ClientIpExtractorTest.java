package com.allanvital.dnsao.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClientIpExtractorTest {

    @Test
    public void xffCommaSeparatedPicksFirstIp() {
        String ip = ClientIpExtractor.extract("1.2.3.4, 5.6.7.8", null, "9.9.9.9");
        assertEquals("1.2.3.4", ip);
    }

    @Test
    public void xffSkipsInvalidTokensAndFindsValidOne() {
        String ip = ClientIpExtractor.extract("garbage, 2.2.2.2", null, "9.9.9.9");
        assertEquals("2.2.2.2", ip);
    }

    @Test
    public void xffBlankFallsBackToXRealIp() {
        String ip = ClientIpExtractor.extract("   ", "3.3.3.3", "9.9.9.9");
        assertEquals("3.3.3.3", ip);
    }

    @Test
    public void xRealIpInvalidFallsBackToRemoteAddr() {
        String ip = ClientIpExtractor.extract(null, "not-an-ip", "4.4.4.4");
        assertEquals("4.4.4.4", ip);
    }

    @Test
    public void realIpInvalidFallsBackToRemoteAddr() {
        String ip = ClientIpExtractor.extract("invalid1,invalid2,1.2.invalid", "not-an-ip", "4.4.4.4");
        assertEquals("4.4.4.4", ip);
    }

    @Test
    public void supportsIpv4WithPortInHeader() {
        String ip = ClientIpExtractor.extract("5.5.5.5:12345", null, "9.9.9.9");
        assertEquals("5.5.5.5", ip);
    }

    @Test
    public void supportsBracketedIpv6WithPortInHeader() {
        String simpleIpv6 = "[2001:db8::1]:443";
        String normalizedIpv6 = "2001:db8:0:0:0:0:0:1";
        String ip = ClientIpExtractor.extract(simpleIpv6, null, "9.9.9.9");
        assertNotNull(ip);
        assertTrue(ip.contains(":"));
        assertEquals(normalizedIpv6, ip);
    }

    @Test
    public void returnsNullWhenAllInputsAreNullOrInvalid() {
        assertNull(ClientIpExtractor.extract(null, null, null));
        assertNull(ClientIpExtractor.extract(" ", " ", "not-an-ip"));
    }

}
