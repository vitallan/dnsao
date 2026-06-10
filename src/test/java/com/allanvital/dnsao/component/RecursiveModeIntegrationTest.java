package com.allanvital.dnsao.component;

import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveModeIntegrationTest extends TestHolder {

    @BeforeEach
    public void setup() throws Exception {
        loadConf("recursive-mode-stub.yml");
        conf.getMisc().setQueryLog(false);
        safeStartWithPresetConf(true);
    }

    @Test
    public void stubDomainReturnsExpectedAnswer() throws IOException {
        Message response = executeRequestOnOwnServer("allanvital.com");

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertTrue(response.getHeader().getFlag(Flags.RA));

        Record question = response.getQuestion();
        assertNotNull(question);
        assertEquals(Name.fromString("allanvital.com."), question.getName());

        Record[] answers = response.getSectionArray(Section.ANSWER);
        assertEquals(1, answers.length);
        assertInstanceOf(ARecord.class, answers[0]);
        ARecord a = (ARecord) answers[0];
        assertEquals("192.0.2.1", a.getAddress().getHostAddress());
    }

    @Test
    public void nonMatchingDomainFallsThroughToServFail() throws IOException {
        Message response = executeRequestOnOwnServer("example.com");

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
    }

    @Test
    public void preservesRdBitFromClientRequest() throws IOException {
        Message request = Message.newQuery(Record.newRecord(Name.fromString("allanvital.com."), Type.A, DClass.IN));
        request.getHeader().setFlag(Flags.RD);

        SimpleResolver resolver = new SimpleResolver("127.0.0.1");
        resolver.setPort(dnsServer.getUdpPort());
        resolver.setTCP(false);

        Message response = resolver.send(request);

        assertNotNull(response);
        assertTrue(response.getHeader().getFlag(Flags.RD));
        assertTrue(response.getHeader().getFlag(Flags.RA));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

}
