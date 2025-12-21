package com.allanvital.dnsao.dnssec;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.pre.handler.DnsPrivacyShaper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.util.LinkedList;
import java.util.List;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.*;
import static com.allanvital.dnsao.dns.processor.pre.handler.opt.Constants.DEFAULT_BLOCK_SIZE;
import static com.allanvital.dnsao.dns.processor.pre.handler.opt.Constants.DEFAULT_UDP_PAYLOAD;
import static com.allanvital.dnsao.graph.bean.MessageHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.xbill.DNS.EDNSOption.Code.NSID;
import static org.xbill.DNS.EDNSOption.Code.PADDING;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsPrivacyShaperTest {

    private static final String domain = "example.com";
    private DnsPrivacyShaper shaper;

    @Test
    public void offModeShouldNotAddPaddingAndShouldNotSetDO() throws Exception {
        shaper = new DnsPrivacyShaper(OFF);

        Message msg = buildARequest(domain, false);
        Message out = shaper.prepare(msg);

        OPTRecord opt = getOpt(out);
        assertNotNull(opt);
        List<EDNSOption> padding = opt.getOptions();
        assertTrue(padding.isEmpty());
        assertFalse(isDO(opt), "dnssec OFF must unset DO");
    }

    @Test
    public void simpleModeShouldAddBlock128Cap96PaddingAndSetDO() throws Exception {
        testWithMessage(SIMPLE, buildARequest(domain, false), 88);
        testWithMessage(SIMPLE, buildARequest("a.com", false), 94);
        testWithMessage(SIMPLE, buildARequest("very-long-sub.domain.example", false), 71);
    }

    @Test
    public void rigidModeShouldFillTheFullUdpPayloadSize() throws Exception {
        testWithMessage(RIGID, buildARequest(domain, false), 1192);
        testWithMessage(RIGID, buildARequest("a.com", false), 1198);
        testWithMessage(RIGID, buildARequest("very-long-sub.domain.example", false), 1175);
    }

    @Test
    void shouldPreserveExistingEdnsOptionsInOPT() throws Exception {
        Message msg = buildARequest(domain, false);
        EDNSOption preExisting = new GenericEDNSOption(NSID, new byte[]{1,2,3});
        List<EDNSOption> preOptions = new LinkedList<>();
        preOptions.add(preExisting);
        OPTRecord preOpt = new OPTRecord(DEFAULT_UDP_PAYLOAD, 0, 0, 0, preOptions);

        EDNSOption nsid = preOpt.getOptions().get(0);
        assertEquals(NSID, nsid.getCode());
        assertEquals(7, nsid.toWire().length);

        msg.addRecord(preOpt, Section.ADDITIONAL);
        DnsPrivacyShaper shaper = new DnsPrivacyShaper(SIMPLE);
        Message out = shaper.prepare(msg);

        OPTRecord opt = out.getOPT();
        Assertions.assertNotNull(opt, "OPT must be present after prepare()");

        List<EDNSOption> options = opt.getOptions();
        assertEquals(2, options.size());

        nsid = options.get(0);
        assertEquals(NSID, nsid.getCode());
        assertEquals(7, nsid.toWire().length); // 4 edns overhead + 3 bytes

        EDNSOption padding = options.get(1);
        assertEquals(PADDING, padding.getCode());
        assertEquals(81, padding.toWire().length); // 88 - nsid.toWire().length
    }

    private void testWithMessage(DNSSecMode mode, Message msg, int expectedPad) throws Exception {
        int cap = 96;
        DnsPrivacyShaper shaper = new DnsPrivacyShaper(mode);

        Message out = shaper.prepare(msg);
        int totalMessageLength = out.toWire().length;
        OPTRecord opt = getOpt(out);
        assertNotNull(opt, "OPT must be present");
        EDNSOption padding = opt.getOptions().get(0);
        assertEquals(12, padding.getCode(), "the single EDNS option must be PADDING (code 12)");
        int actualPaddingLength = padding.toWire().length;

        if (SIMPLE.equals(mode)) {
            assertTrue(actualPaddingLength <= cap, "padding <= 96 (cap)");
            assertEquals(0, totalMessageLength % DEFAULT_BLOCK_SIZE, "message size should be multiple of 128");
        }
        if (RIGID.equals(mode)) {
            assertEquals(1232, totalMessageLength, "in rigid, should always fill the package");
        }
        assertTrue(isDO(opt), "dnssec SIMPLE/RIGID must set DO");
        assertEquals(expectedPad, actualPaddingLength);
    }

}
