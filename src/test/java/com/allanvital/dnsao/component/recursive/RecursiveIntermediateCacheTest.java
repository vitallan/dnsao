package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.dns.recursive.RootHintsProvider;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveIntermediateCacheTest extends TestHolder {

    private static final String DOMAIN = "allanvital.com";
    private static final String COM_NS_HOST = "ns1.com";
    private static final String COM_NS_IP = "127.0.0.10";
    private static final String AUTH_NS_HOST = "ns1.allanvital.com";
    private static final String AUTH_NS_IP = "127.0.0.11";
    private static final String FINAL_IPV4 = "10.0.0.31";
    private static final String FINAL_IPV6 = "2001:db8::31";
    private static final long FINAL_TTL = 300;

    private CacheManager cacheManager;
    private FakeServer authoritativeServer;

    @Override
    protected void setRootHints() throws ConfException {
        RootHintsProvider fakeHints = new RootHintsProvider() {
            @Override
            public List<NameServerAddress> getRootServers() {
                return List.of(new NameServerAddress("127.0.0.1", fakeUpstreamServer.getPort()));
            }
        };
        registerOverride(fakeHints);
    }

    @BeforeEach
    public void setup() throws Exception {
        authoritativeServer = new FakeUdpServer(0);
        authoritativeServer.start();

        loadConf("recursive-mode-cache.yml");
        conf.getMisc().setQueryLog(false);
        safeStartWithPresetConf();

        cacheManager = assembler.getCacheManager();
        TestStepResolverFactory stepResolverFactory = queryInfraAssembler.getTestStepResolverFactory();
        stepResolverFactory.setPortToUse(fakeUpstreamServer.getPort());
        stepResolverFactory.setRoute(COM_NS_IP, authoritativeServer.getPort());
        stepResolverFactory.setRoute(AUTH_NS_IP, authoritativeServer.getPort());
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
        if (authoritativeServer != null) {
            authoritativeServer.stop();
        }
    }

    @Test
    public void storesIntermediateNsEntriesAfterFirstRecursiveQuery() throws Exception {
        loadGlueScenario(300);

        Message response = executeRequestOnOwnServer(buildQuery(DOMAIN, Type.A));

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertCacheEntryPresent("NS:com.");
        assertCacheEntryPresent("NS:" + DOMAIN + ".");
    }

    @Test
    public void reusesCachedIntermediateNsEntriesForDifferentQtypeOnSameDomain() throws Exception {
        loadGlueScenario(300);

        Message firstResponse = executeRequestOnOwnServer(buildQuery(DOMAIN, Type.A));
        assertNotNull(firstResponse);
        assertEquals(Rcode.NOERROR, firstResponse.getRcode());

        fakeUpstreamServer.clearReceivedQueries();
        authoritativeServer.clearReceivedQueries();

        Message secondResponse = executeRequestOnOwnServer(buildQuery(DOMAIN, Type.AAAA));

        assertNotNull(secondResponse);
        assertEquals(Rcode.NOERROR, secondResponse.getRcode());
        assertEquals(List.of(), fakeUpstreamServer.getReceivedQueries());
        assertEquals(List.of(key(DOMAIN, Type.AAAA)), authoritativeServer.getReceivedQueries());
    }

    @Test
    public void removesExpiredIntermediateNsEntriesAndQueriesThemAgain() throws Exception {
        loadGlueScenario(1);

        Message firstResponse = executeRequestOnOwnServer(buildQuery(DOMAIN, Type.A));
        assertNotNull(firstResponse);
        assertEquals(Rcode.NOERROR, firstResponse.getRcode());
        assertCacheEntryPresent("NS:com.");
        assertCacheEntryPresent("NS:" + DOMAIN + ".");

        fakeUpstreamServer.clearReceivedQueries();
        authoritativeServer.clearReceivedQueries();

        testTimeProvider.walkNow(1500);
        waitUntilCacheEntryAbsent("NS:com.");
        waitUntilCacheEntryAbsent("NS:" + DOMAIN + ".");

        Message secondResponse = executeRequestOnOwnServer(buildQuery(DOMAIN, Type.AAAA));

        assertNotNull(secondResponse);
        assertEquals(Rcode.NOERROR, secondResponse.getRcode());
        assertEquals(List.of(key("com", Type.NS), key(DOMAIN, Type.NS)), fakeUpstreamServer.getReceivedQueries());
        assertEquals(List.of(key(DOMAIN, Type.AAAA)), authoritativeServer.getReceivedQueries());
    }

    private void loadGlueScenario(long nsTtl) throws Exception {
        fakeUpstreamServer.clearReceivedQueries();
        authoritativeServer.clearReceivedQueries();

        Message comNsQuery = buildQuery("com", Type.NS);
        fakeUpstreamServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, COM_NS_HOST, COM_NS_IP, nsTtl));

        Message domainNsQuery = buildQuery(DOMAIN, Type.NS);
        fakeUpstreamServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, AUTH_NS_HOST, AUTH_NS_IP, nsTtl));

        Message domainAQuery = buildQuery(DOMAIN, Type.A);
        authoritativeServer.mockResponse(domainAQuery, buildAResponse(domainAQuery, FINAL_IPV4, FINAL_TTL));

        Message domainAaaaQuery = buildQuery(DOMAIN, Type.AAAA);
        authoritativeServer.mockResponse(domainAaaaQuery, com.allanvital.dnsao.graph.bean.MessageHelper.buildAaaaResponse(domainAaaaQuery, FINAL_IPV6, FINAL_TTL));
    }

    private void assertCacheEntryPresent(String key) {
        if (cacheManager.safeGet(key) == null) {
            fail("expected cache entry to exist: " + key);
        }
    }

    private void waitUntilCacheEntryAbsent(String key) throws Exception {
        long timeoutMs = 5000L;
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cacheManager.safeGet(key) == null) {
                return;
            }
            Thread.sleep(20);
        }
        fail("cache entry was not removed in time: " + key);
    }

    private Message buildQuery(String qname, int qtype) throws TextParseException {
        String normalized = qname.endsWith(".") ? qname : qname + ".";
        return Message.newQuery(Record.newRecord(Name.fromString(normalized), qtype, DClass.IN));
    }

    private Message buildNsReferralWithGlueResponse(Message request, String nsHost, String nsIp, long ttl) throws Exception {
        Message response = buildNoDataResponse(request);
        Record question = request.getQuestion();
        if (question == null) {
            return response;
        }
        String normalizedNsHost = nsHost.endsWith(".") ? nsHost : nsHost + ".";
        Name nsName = Name.fromString(normalizedNsHost);
        response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, nsName), Section.AUTHORITY);
        response.addRecord(new ARecord(nsName, DClass.IN, ttl, InetAddress.getByName(nsIp)), Section.ADDITIONAL);
        return response;
    }

    private Message buildAResponse(Message request, String ipAddress, long ttl) throws Exception {
        Message response = buildNoDataResponse(request);
        Record question = request.getQuestion();
        if (question != null && question.getType() == Type.A) {
            response.addRecord(new ARecord(question.getName(), question.getDClass(), ttl, InetAddress.getByName(ipAddress)), Section.ANSWER);
        }
        return response;
    }

    private Message buildNoDataResponse(Message request) {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setOpcode(request.getHeader().getOpcode());
        response.getHeader().setFlag(org.xbill.DNS.Flags.QR);
        if (request.getHeader().getFlag(org.xbill.DNS.Flags.RD)) {
            response.getHeader().setFlag(org.xbill.DNS.Flags.RD);
        }
        if (request.getQuestion() != null) {
            response.addRecord(request.getQuestion(), Section.QUESTION);
        }
        response.getHeader().setRcode(Rcode.NOERROR);
        return response;
    }

    private DnsQueryKey key(String qname, int qtype) throws TextParseException {
        String normalized = qname.endsWith(".") ? qname : qname + ".";
        return new DnsQueryKey(Name.fromString(normalized), qtype, DClass.IN);
    }

}
