package com.allanvital.dnsao.component;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.cache.pojo.DnsCacheEntry;
import com.allanvital.dnsao.cache.rewarm.FixedTimeRewarmScheduler;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.TestResolverProvider;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.allanvital.dnsao.dns.processor.engine.unit.AbstractCacheUnit.key;
import static com.allanvital.dnsao.infra.notification.telemetry.EventType.CACHE_REWARM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RewarmHttpsNodataTest extends TestHolder {

    private static final String DOMAIN = "update.googleapis.com";

    private CacheManager cacheManager;

    @BeforeEach
    public void setup() throws ConfException {
        registerOverride(new FixedTimeRewarmScheduler(1000));
        registerOverride(new TestResolverProvider(List.of(new HttpsThenNodataResolver())));
        safeStart("1udp-upstream-cache-rewarm.yml");
        cacheManager = assembler.getCacheManager();
    }

    @Test
    public void rewarmShouldStoreHttpsNoErrorEmptyAnswerWithSoa() throws Exception {
        Message request = buildHttpsRequest(DOMAIN);

        Message initialResponse = executeCustomRequest(request);
        assertEquals(Rcode.NOERROR, initialResponse.getRcode());
        assertEquals(1, initialResponse.getSection(Section.ANSWER).size());

        testTimeProvider.walkNow(1200L);
        eventListener.assertCount(CACHE_REWARM, 1, false);

        DnsCacheEntry entry = cacheManager.safeGet(key(request));
        assertNotNull(entry);
        assertEquals(Rcode.NOERROR, entry.getResponse().getRcode());
        assertEquals(0, entry.getResponse().getSection(Section.ANSWER).size());
        assertEquals(1, entry.getResponse().getSection(Section.AUTHORITY).size());
        assertEquals(Type.SOA, entry.getResponse().getSection(Section.AUTHORITY).get(0).getType());
        assertEquals(60L, entry.getConfiguredTtlInSeconds());
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        safeStop();
    }

    private Message executeCustomRequest(Message request) throws IOException {
        SimpleResolver resolver = new SimpleResolver(LOCAL);
        resolver.setPort(dnsServer.getUdpPort());
        resolver.setTCP(false);
        return resolver.send(request);
    }

    private static Message buildHttpsRequest(String domain) throws TextParseException {
        String qname = domain.endsWith(".") ? domain : domain + ".";
        return Message.newQuery(Record.newRecord(Name.fromString(qname), Type.HTTPS, DClass.IN));
    }

    private static Message buildHttpsResponse(Message request, long ttl) throws TextParseException {
        Message response = baseResponseFrom(request);
        Record question = request.getQuestion();
        HTTPSRecord answer = new HTTPSRecord(question.getName(), question.getDClass(), ttl, 1, Name.fromString("."), List.of());
        response.addRecord(answer, Section.ANSWER);
        return response;
    }

    private static Message buildNoErrorEmptyWithSoa(Message request, long ttl, long minimum) throws TextParseException {
        Message response = baseResponseFrom(request);
        SOARecord soaRecord = new SOARecord(
                Name.fromString("googleapis.com."),
                DClass.IN,
                ttl,
                Name.fromString("ns1.google.com."),
                Name.fromString("dns-admin.google.com."),
                950017825L,
                900L,
                900L,
                1800L,
                minimum
        );
        response.addRecord(soaRecord, Section.AUTHORITY);
        return response;
    }

    private static Message baseResponseFrom(Message request) {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setFlag(Flags.RA);
        if (request.getHeader().getFlag(Flags.RD)) {
            response.getHeader().setFlag(Flags.RD);
        }
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(request.getQuestion(), Section.QUESTION);
        return response;
    }

    private static class HttpsThenNodataResolver implements UpstreamResolver {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String getIp() {
            return "test-upstream";
        }

        @Override
        public int getPort() {
            return 53;
        }

        @Override
        public String name() {
            return "test-upstream";
        }

        @Override
        public Message send(Message query) throws IOException {
            try {
                if (calls.incrementAndGet() == 1) {
                    return buildHttpsResponse(query, 2L);
                }
                return buildNoErrorEmptyWithSoa(query, 60L, 60L);
            } catch (TextParseException e) {
                throw new IOException(e);
            }
        }
    }
}
