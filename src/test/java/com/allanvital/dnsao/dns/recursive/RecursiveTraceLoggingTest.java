package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.infra.log.Log;
import com.allanvital.dnsao.infra.log.LogConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveTraceLoggingTest {

    private static final String DOMAIN = "allanvital.com";
    private static final String ROOT_SERVER_IP = "127.0.0.10";
    private static final String COM_SERVER_IP = "127.0.0.11";
    private static final String AUTH_SERVER_IP = "127.0.0.12";
    private static final String COM_NS_HOST = "ns1.com";
    private static final String COM_NS_IP = "127.0.0.71";
    private static final String AUTH_NS_HOST = "ns1.allanvital.com";
    private static final String AUTH_NS_IP = "127.0.0.72";
    private static final String FINAL_IP = "10.0.0.91";
    private static final long TTL = 300;

    private final List<String> captured = new ArrayList<>();
    private ExecutorService executorService;

    @BeforeEach
    public void setUp() throws Exception {
        Log.setHandler((level, category, message) -> captured.add(category + ":" + message));
        LogConfigurator.reset();
        com.allanvital.dnsao.conf.inner.LogConf conf = new com.allanvital.dnsao.conf.inner.LogConf();
        conf.setDns("TRACE");
        conf.setCache("TRACE");
        conf.setInfra("TRACE");
        conf.setRootLevel("TRACE");
        LogConfigurator.configure(conf);

        executorService = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        LogConfigurator.reset();
        Log.setHandler(null);
    }

    @Test
    public void tracesRecursiveResolutionSteps() throws Exception {
        RecursiveSession session = buildSession(new NoOpRecursiveStatsCollector(), new RecursiveCache(null));

        Message response = session.resolve();

        assertNotNull(response);
        assertTrue(contains("DNS:recursive start session="));
        assertTrue(contains("phase=walk qtype=NS qname=com."));
        assertTrue(contains("phase=walk qtype=NS qname=allanvital.com."));
        assertTrue(contains("phase=final qtype=A qname=allanvital.com."));
        assertTrue(contains("DNS:recursive done session="));
    }

    private RecursiveSession buildSession(RecursiveStatsCollector recursiveStatsCollector, RecursiveCache recursiveCache) throws Exception {
        FixtureStepResolverFactory stepResolverFactory = new FixtureStepResolverFactory(recursiveStatsCollector);

        Message comNsQuery = buildQuery("com", Type.NS);
        Message domainNsQuery = buildQuery(DOMAIN, Type.NS);
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);

        stepResolverFactory.mock(ROOT_SERVER_IP, comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, COM_NS_HOST, COM_SERVER_IP));
        stepResolverFactory.mock(ROOT_SERVER_IP, domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, AUTH_NS_HOST, AUTH_SERVER_IP));
        stepResolverFactory.mock(AUTH_SERVER_IP, domainAQuery, MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL));

        RootHintsProvider rootHintsProvider = new RootHintsProvider() {
            @Override
            public List<NameServerAddress> getRootServers() {
                return List.of(new NameServerAddress(ROOT_SERVER_IP, 53));
            }
        };

        ServerRacer serverRacer = new ServerRacer(executorService, 1, stepResolverFactory, new DnssecDowngradeHandler(DNSSecMode.OFF, recursiveStatsCollector), recursiveStatsCollector);
        DnsQueryRequest dnsQueryRequest = new DnsQueryRequest(java.net.InetAddress.getLoopbackAddress());
        dnsQueryRequest.setRequest(MessageHelper.buildARequest(DOMAIN));
        return new RecursiveSession(dnsQueryRequest, serverRacer, rootHintsProvider, recursiveCache, DNSSecMode.OFF, recursiveStatsCollector);
    }

    private Message buildNsReferralWithGlueResponse(Message request, String nsHost, String nsIp) throws Exception {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setOpcode(request.getHeader().getOpcode());
        response.getHeader().setFlag(org.xbill.DNS.Flags.QR);
        response.addRecord(request.getQuestion(), org.xbill.DNS.Section.QUESTION);
        response.getHeader().setRcode(org.xbill.DNS.Rcode.NOERROR);
        org.xbill.DNS.Name nsName = org.xbill.DNS.Name.fromString(nsHost.endsWith(".") ? nsHost : nsHost + ".");
        response.addRecord(new org.xbill.DNS.NSRecord(request.getQuestion().getName(), request.getQuestion().getDClass(), TTL, nsName), org.xbill.DNS.Section.AUTHORITY);
        response.addRecord(new org.xbill.DNS.ARecord(nsName, org.xbill.DNS.DClass.IN, TTL, InetAddress.getByName(nsIp)), org.xbill.DNS.Section.ADDITIONAL);
        return response;
    }

    private boolean contains(String fragment) {
        for (String line : captured) {
            if (line.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private Message buildQuery(String qname, int qtype) {
        try {
            String normalized = qname.endsWith(".") ? qname : qname + ".";
            return Message.newQuery(Record.newRecord(Name.fromString(normalized), qtype, DClass.IN));
        } catch (TextParseException e) {
            throw new IllegalStateException("failed to build query " + qname, e);
        }
    }

    private static final class FixtureStepResolverFactory extends StepResolverFactory {

        private final Map<String, StepResponse> responsesByKey = new HashMap<>();

        private FixtureStepResolverFactory(RecursiveStatsCollector recursiveStatsCollector) {
            super(1, recursiveStatsCollector);
        }

        public void mock(String serverIp, Message query, Message response) {
            responsesByKey.put(key(serverIp, query.getQuestion().getName(), query.getQuestion().getType(), query.getQuestion().getDClass()), new StepResponse(response));
        }

        @Override
        public StepResolver create(String ip, int port) {
            return request -> {
                StepResponse stepResponse = responsesByKey.get(key(ip, request.qname(), request.qtype(), request.qclass()));
                if (stepResponse == null) {
                    throw new IllegalStateException("missing fixture response for " + key(ip, request.qname(), request.qtype(), request.qclass()));
                }
                return stepResponse;
            };
        }

        private String key(String serverIp, Name qname, int qtype, int qclass) {
            return serverIp + "|" + qname + "|" + qtype + "|" + qclass;
        }
    }

}
