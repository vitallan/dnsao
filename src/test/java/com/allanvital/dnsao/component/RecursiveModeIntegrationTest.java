package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.processor.engine.EngineUnitProvider;
import com.allanvital.dnsao.dns.processor.engine.unit.RecursiveUnit;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.dns.recursive.RootHintsProvider;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveModeIntegrationTest extends TestHolder {

    private static final String DOMAIN = "allanvital.com";
    private static final String DOMAIN_DOT = DOMAIN + ".";

    private static final String IP = "192.0.2.1";

    private final List<QueryEvent> eventsReceived = new LinkedList<>();

    @BeforeEach
    public void setup() throws Exception {
        setupAndStartFakeRootServer();
        safeStart("recursive-mode-stub.yml");
        NotificationManager notificationManager = assembler.getNotificationManager();
        notificationManager.querySubscribe(eventsReceived::add);
        Message request = MessageHelper.buildARequest(DOMAIN);
        Message response = MessageHelper.buildAResponse(request, IP, 60);
        fakeRootServer.mockResponse(request, response);
    }

    @Test
    public void stubDomainReturnsExpectedAnswer() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertTrue(response.getHeader().getFlag(Flags.RA));

        Record question = response.getQuestion();
        assertNotNull(question);
        assertEquals(Name.fromString(DOMAIN_DOT), question.getName());
        List<Record> responseRecords = response.getSection(Section.ANSWER);
        assertEquals(1, responseRecords.size());
        assertInstanceOf(ARecord.class, responseRecords.get(0));
        ARecord a = (ARecord) responseRecords.get(0);
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
        Message request = Message.newQuery(Record.newRecord(Name.fromString(DOMAIN_DOT), Type.A, DClass.IN));
        request.getHeader().setFlag(Flags.RD);

        Message response = executeRequestOnOwnServer(request);

        assertNotNull(response);
        assertTrue(response.getHeader().getFlag(Flags.RD));
        assertTrue(response.getHeader().getFlag(Flags.RA));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }

    @Test
    public void recursiveUnitReportsRecursion() throws Exception {
        executeRequestOnOwnServer(DOMAIN);
        waitEvent();
        QueryEvent queryEvent = eventsReceived.get(0);
        assertEquals(QueryResolvedBy.RECURSION, queryEvent.getQueryResolvedBy());
    }

    private void waitEvent() throws Exception {
        long timeoutMs = 5000L;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!eventsReceived.isEmpty()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("Query event not received in expected time ");
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

}
