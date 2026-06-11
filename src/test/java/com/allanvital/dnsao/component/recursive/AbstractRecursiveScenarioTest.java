package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.recursive.NameServerAddress;
import com.allanvital.dnsao.dns.recursive.RootHintsProvider;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpTcpDnsServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.infra.notification.NotificationManager;
import com.allanvital.dnsao.infra.notification.QueryEvent;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.xbill.DNS.Message;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class AbstractRecursiveScenarioTest extends TestHolder {

    private final List<FakeServer> extraFakeServers = new ArrayList<>();
    protected final List<QueryEvent> eventsReceived = new LinkedList<>();

    @BeforeEach
    public final void setupRecursiveScenarioBase() throws Exception {
        beforeServerStart();
        loadConf("recursive-mode-stub.yml");
        conf.getMisc().setQueryLog(false);
        conf.getMisc().setDnssec(recursiveDnssecMode().name());
        if (fakeUpstreamServer == null) {
            safeStartWithPresetConf();
        } else {
            safeStartWithPresetConf(true);
        }

        TestStepResolverFactory stepResolverFactory = queryInfraAssembler.getTestStepResolverFactory();
        stepResolverFactory.clearRoutes();
        stepResolverFactory.setPortToUse(fakeUpstreamServer.getPort());
        configureResolverRouting(stepResolverFactory);

        NotificationManager notificationManager = assembler.getNotificationManager();
        notificationManager.querySubscribe(eventsReceived::add);
        fakeUpstreamServer.clearReceivedQueries();

        afterServerStart();
    }

    @AfterEach
    public final void tearDownRecursiveScenarioBase() throws Exception {
        safeStop();
        for (FakeServer extraFakeServer : extraFakeServers) {
            extraFakeServer.stop();
        }
        extraFakeServers.clear();
        eventsReceived.clear();
    }

    @Override
    protected void setRootHints() throws com.allanvital.dnsao.exc.ConfException {
        RootHintsProvider fakeHints = new RootHintsProvider() {
            @Override
            public List<NameServerAddress> getRootServers() {
                return rootHintServers();
            }
        };
        registerOverride(fakeHints);
    }

    protected List<NameServerAddress> rootHintServers() {
        return List.of(new NameServerAddress("127.0.0.1", fakeUpstreamServer.getPort()));
    }

    protected void beforeServerStart() throws Exception {
    }

    protected void afterServerStart() throws Exception {
    }

    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
    }

    protected DNSSecMode recursiveDnssecMode() {
        return DNSSecMode.SIMPLE;
    }

    protected FakeServer startFakeUdpServer() throws Exception {
        FakeServer server = new FakeUdpServer(0);
        server.start();
        return server;
    }

    protected FakeServer startSilentFakeUdpServer() throws Exception {
        FakeServer server = new SilentFakeUdpServer(0);
        server.start();
        return server;
    }

    protected FakeUdpTcpDnsServer startFakeUdpTcpDnsServer() throws Exception {
        FakeUdpTcpDnsServer server = new FakeUdpTcpDnsServer(0);
        server.start();
        return server;
    }

    protected void trackExtraFakeServer(FakeServer server) {
        extraFakeServers.add(server);
    }

    protected void assertReceivedQueries(List<DnsQueryKey> expectedQueries) {
        assertEquals(expectedQueries, fakeUpstreamServer.getReceivedQueries());
    }

    protected void assertReceivedQueries(FakeServer server, List<DnsQueryKey> expectedQueries) {
        assertEquals(expectedQueries, server.getReceivedQueries());
    }

    protected List<Message> getReceivedMessages(FakeServer server) {
        return server.getReceivedMessages();
    }

    protected void assertReceivedUdpQueries(FakeUdpTcpDnsServer server, List<DnsQueryKey> expectedQueries) {
        assertEquals(expectedQueries, server.getReceivedUdpQueries());
    }

    protected void assertReceivedTcpQueries(FakeUdpTcpDnsServer server, List<DnsQueryKey> expectedQueries) {
        assertEquals(expectedQueries, server.getReceivedTcpQueries());
    }

    protected void assertRecursiveResolvedEvent() {
        assertEquals(1, eventsReceived.size());
        assertEquals(QueryResolvedBy.RECURSION, eventsReceived.get(0).getQueryResolvedBy());
    }
}
