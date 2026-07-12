package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static com.allanvital.dnsao.graph.bean.MessageHelper.buildARequest;
import static org.junit.jupiter.api.Assertions.*;

class HeaderCleanerTest {

    @Test
    void shouldNormalizeWeirdHeaderBitsAndPreserveCdInSimple() throws Exception {
        Message message = buildARequest("example.com");
        message.getHeader().setFlag(Flags.QR);
        message.getHeader().setFlag(Flags.AA);
        message.getHeader().setFlag(Flags.RA);
        message.getHeader().setFlag(Flags.TC);
        message.getHeader().setFlag(Flags.AD);
        message.getHeader().setFlag(Flags.CD);
        message.getHeader().unsetFlag(Flags.RD);
        message.getHeader().setRcode(Rcode.SERVFAIL);

        Message out = new HeaderCleaner(DNSSecMode.SIMPLE).prepare(message);

        assertFalse(out.getHeader().getFlag(Flags.QR));
        assertFalse(out.getHeader().getFlag(Flags.AA));
        assertFalse(out.getHeader().getFlag(Flags.RA));
        assertFalse(out.getHeader().getFlag(Flags.TC));
        assertFalse(out.getHeader().getFlag(Flags.AD));
        assertTrue(out.getHeader().getFlag(Flags.CD));
        assertTrue(out.getHeader().getFlag(Flags.RD));
        assertEquals(Rcode.NOERROR, out.getRcode());
    }

    @Test
    void shouldForceRdInRigid() throws Exception {
        Message message = buildARequest("example.com");
        message.getHeader().unsetFlag(Flags.RD);

        Message out = new HeaderCleaner(DNSSecMode.RIGID).prepare(message);

        assertTrue(out.getHeader().getFlag(Flags.RD));
    }
}
