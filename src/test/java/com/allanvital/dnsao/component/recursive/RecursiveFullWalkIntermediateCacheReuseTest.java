package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.component.fixture.recursive.AbstractRecursiveScenarioFixture;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.dns.recursive.RootHintsProvider;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveFullWalkIntermediateCacheReuseTest extends TestHolder {

    private static final String FIRST_DOMAIN = "www.dev.example.com";
    private static final String SECOND_DOMAIN = "api.dev.example.com";
    private static final String EXAMPLE_ZONE = "example.com";
    private static final String DEV_ZONE = "dev.example.com";
    private static final String COM_NS_HOST = "ns1.com";
    private static final String COM_NS_IP = "127.0.0.221";
    private static final String EXAMPLE_NS_HOST = "ns1.example.com";
    private static final String EXAMPLE_NS_IP = "127.0.0.222";
    private static final String DEV_NS_HOST = "ns1.dev.example.com";
    private static final String DEV_NS_IP = "127.0.0.223";
    private static final String FINAL_IP = "10.0.0.91";
    private static final long TTL = 300;

    private CacheManager cacheManager;
    private FakeServer delegatedServer;

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
        delegatedServer = new FakeUdpServer(0);
        delegatedServer.start();

        loadConf("recursive-mode-cache.yml");
        conf.getMisc().setQueryLog(false);
        safeStartWithPresetConf();

        cacheManager = assembler.getCacheManager();
        TestStepResolverFactory stepResolverFactory = queryInfraAssembler.getTestStepResolverFactory();
        stepResolverFactory.setPortToUse(fakeUpstreamServer.getPort());
        stepResolverFactory.setRoute(DEV_NS_IP, delegatedServer.getPort());
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
        if (delegatedServer != null) {
            delegatedServer.stop();
        }
    }

    @Test
    public void reusesCachedIntermediateZoneCutsAcrossQueriesUnderSameDelegatedZone() throws Exception {
        FixtureHelper fixture = loadScenario();

        Message firstResponse = executeRequestOnOwnServer(buildQuery(FIRST_DOMAIN, Type.A));
        assertNotNull(firstResponse);
        assertEquals(org.xbill.DNS.Rcode.NOERROR, firstResponse.getRcode());
        assertCacheEntryPresent("NS:com.");
        assertCacheEntryPresent("NS:" + EXAMPLE_ZONE + ".");
        assertCacheEntryPresent("NS:" + DEV_ZONE + ".");

        fakeUpstreamServer.clearReceivedQueries();
        delegatedServer.clearReceivedQueries();

        Message secondResponse = executeRequestOnOwnServer(buildQuery(SECOND_DOMAIN, Type.A));

        assertNotNull(secondResponse);
        assertEquals(org.xbill.DNS.Rcode.NOERROR, secondResponse.getRcode());
        assertEquals(List.of(), fakeUpstreamServer.getReceivedQueries());
        assertEquals(List.of(fixture.key(SECOND_DOMAIN, Type.A)), delegatedServer.getReceivedQueries());
    }

    private FixtureHelper loadScenario() throws Exception {
        fakeUpstreamServer.clearReceivedQueries();
        delegatedServer.clearReceivedQueries();

        FixtureHelper fixture = new FixtureHelper(fakeUpstreamServer);

        Message comNsQuery = fixture.request("com", Type.NS);
        fakeUpstreamServer.mockResponse(comNsQuery, fixture.referralWithGlue(comNsQuery, COM_NS_HOST, COM_NS_IP, TTL));

        Message exampleNsQuery = fixture.request(EXAMPLE_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(exampleNsQuery, fixture.referralWithGlue(exampleNsQuery, EXAMPLE_NS_HOST, EXAMPLE_NS_IP, TTL));

        Message devNsQuery = fixture.request(DEV_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(devNsQuery, fixture.referralWithGlue(devNsQuery, DEV_NS_HOST, DEV_NS_IP, TTL));

        Message firstDomainAQuery = MessageHelper.buildARequest(FIRST_DOMAIN);
        delegatedServer.mockResponse(firstDomainAQuery, MessageHelper.buildAResponse(firstDomainAQuery, FINAL_IP, TTL));

        Message secondDomainAQuery = MessageHelper.buildARequest(SECOND_DOMAIN);
        delegatedServer.mockResponse(secondDomainAQuery, MessageHelper.buildAResponse(secondDomainAQuery, FINAL_IP, TTL));

        return fixture;
    }

    private void assertCacheEntryPresent(String key) {
        assertNotNull(cacheManager.safeGet(key), "expected cache entry to exist: " + key);
    }

    private Message buildQuery(String qname, int qtype) throws TextParseException {
        String normalized = qname.endsWith(".") ? qname : qname + ".";
        return Message.newQuery(Record.newRecord(Name.fromString(normalized), qtype, DClass.IN));
    }

    private static final class FixtureHelper extends AbstractRecursiveScenarioFixture {

        private FixtureHelper(FakeServer fakeServer) {
            super(fakeServer);
        }

        private Message request(String qname, int qtype) {
            return buildRequest(qname, qtype);
        }

        private Message referralWithGlue(Message request, String nsHost, String nsIp, long ttl) {
            return buildNsReferralWithGlueResponse(request, nsHost, nsIp, ttl);
        }

        protected DnsQueryKey key(String qname, int qtype) {
            return super.key(qname, qtype);
        }
    }
}
