package com.allanvital.dnsao.dns.processor.pre.handler;

import org.junit.jupiter.api.Test;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Section;

import static com.allanvital.dnsao.graph.bean.MessageHelper.buildARequest;
import static org.junit.jupiter.api.Assertions.*;

class OptCleanerTest {

    @Test
    void shouldRemoveAllOptRecords() throws Exception {
        Message message = buildARequest("example.com");
        message.addRecord(new OPTRecord(1232, 0, 0, 0), Section.ADDITIONAL);
        message.addRecord(new OPTRecord(1232, 0, 0, ExtendedFlags.DO), Section.ADDITIONAL);

        Message out = new OptCleaner().prepare(message);

        long optCount = out.getSection(Section.ADDITIONAL).stream().filter(OPTRecord.class::isInstance).count();
        assertEquals(0, optCount);
    }
}
