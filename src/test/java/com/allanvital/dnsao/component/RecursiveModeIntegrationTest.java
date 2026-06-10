package com.allanvital.dnsao.component;

import com.allanvital.dnsao.component.fixture.RecursiveScenarioFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveModeIntegrationTest extends TestHolder {

    private static final String DOMAIN = "allanvital.com";
    private static final String DOMAIN_DOT = DOMAIN + ".";
    private static final String IP = "192.0.2.1";
    private static final String LOOPBACK_IP = "127.0.0.1";
    private static final String COM_NS_HOST = "ns1.com";
    private static final long REFERRAL_TTL = 300;

    private final List<QueryEvent> eventsReceived = new LinkedList<>();
    private RecursiveScenarioFixture recursiveScenarioFixture;
    private List<DnsQueryKey> expectedQueries;

    @BeforeEach
    public void setup() throws Exception {
        safeStart("recursive-mode-stub.yml");

        queryInfraAssembler.getTestStepResolverFactory().setPortToUse(fakeUpstreamServer.getPort());

        NotificationManager notificationManager = assembler.getNotificationManager();
        notificationManager.querySubscribe(eventsReceived::add);

        recursiveScenarioFixture = new RecursiveScenarioFixture(fakeUpstreamServer);
        expectedQueries = recursiveScenarioFixture.loadMinifiedHappyPath(DOMAIN, IP, COM_NS_HOST, LOOPBACK_IP, REFERRAL_TTL);
    }

    @Test
    public void stubDomainReturnsExpectedAnswer() throws IOException {
        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertTrue(response.getHeader().getFlag(Flags.RA));

        String responseIp = MessageHelper.extractIpFromResponseMessage(response);
        assertEquals(IP, responseIp);

        Record question = response.getQuestion();
        assertNotNull(question);
        assertEquals(Name.fromString(DOMAIN_DOT), question.getName());

        assertReceivedQueries(expectedQueries);
    }

    @Test
    public void nonMatchingDomainFallsThroughToServFail() throws IOException {
        expectedQueries = recursiveScenarioFixture.loadMinifiedServfailPath("example.com", COM_NS_HOST, LOOPBACK_IP, REFERRAL_TTL);

        Message response = executeRequestOnOwnServer("example.com");

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());
        assertTrue(response.getHeader().getFlag(Flags.QR));
        assertReceivedQueries(expectedQueries);
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
        assertReceivedQueries(expectedQueries);
    }

    @Test
    public void recursiveUnitReportsRecursion() throws Exception {
        executeRequestOnOwnServer(DOMAIN);
        waitEvent();
        QueryEvent queryEvent = eventsReceived.get(0);
        assertEquals(QueryResolvedBy.RECURSION, queryEvent.getQueryResolvedBy());
        assertReceivedQueries(expectedQueries);
    }

    @Test
    public void resolvesNsWithoutGlue() throws Exception {
        expectedQueries = recursiveScenarioFixture.loadMinifiedNsWithoutGlue(DOMAIN, COM_NS_HOST, LOOPBACK_IP, "5.6.7.8", REFERRAL_TTL);

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        List<Record> answers = response.getSection(Section.ANSWER);
        assertEquals(1, answers.size());
        ARecord a = (ARecord) answers.get(0);
        assertEquals("5.6.7.8", a.getAddress().getHostAddress());
        assertReceivedQueries(expectedQueries);
    }

    private void assertReceivedQueries(List<DnsQueryKey> expectedQueries) {
        assertEquals(expectedQueries, fakeUpstreamServer.getReceivedQueries());
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
        eventsReceived.clear();
    }

}
