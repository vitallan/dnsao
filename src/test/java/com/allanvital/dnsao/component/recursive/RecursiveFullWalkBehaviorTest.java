package com.allanvital.dnsao.component.recursive;

import com.allanvital.dnsao.component.fixture.recursive.AbstractRecursiveScenarioFixture;
import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.bean.TestStepResolverFactory;
import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.fake.FakeUdpServer;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveFullWalkBehaviorTest extends AbstractRecursiveScenarioTest {

    private static final String DOMAIN = "www.dev.example.com";
    private static final String SECOND_DOMAIN = "api.dev.example.com";
    private static final String NXDOMAIN = "missing.dev.example.com";
    private static final String NODATA = "nodata.dev.example.com";
    private static final String LOOP_DOMAIN = "loop.dev.example.com";
    private static final String CNAME_TARGET = "edge.dev.example.com";
    private static final String EXAMPLE_ZONE = "example.com";
    private static final String DEV_ZONE = "dev.example.com";
    private static final String DEV_HELPER_ZONE = "dev-helper.com";
    private static final String COM_NS_HOST = "ns1.com";
    private static final String COM_NS_IP = "127.0.0.201";
    private static final String EXAMPLE_NS_HOST = "ns1.example.com";
    private static final String EXAMPLE_NS_IP = "127.0.0.202";
    private static final String DEV_NS_HOST = "ns1.dev.example.com";
    private static final String DEV_NS_IP = "127.0.0.210";
    private static final String BAD_NS_HOST = "ns1.bad.example.com";
    private static final String BAD_NS_IP = "127.0.0.211";
    private static final String DEV_HELPER_NS_HOST = "ns1.dev-helper.com";
    private static final String DEV_HELPER_NS_IP = "127.0.0.212";
    private static final String FINAL_IP = "10.0.0.81";
    private static final String BAD_FINAL_IP = "10.0.0.82";
    private static final String CNAME_FINAL_IP = "10.0.0.83";
    private static final long TTL = 300;

    private FakeServer delegatedServer;
    private FakeServer badDelegatedServer;

    @Override
    protected void beforeServerStart() throws Exception {
        delegatedServer = new FakeUdpServer(0);
        delegatedServer.start();
        trackExtraFakeServer(delegatedServer);

        badDelegatedServer = new FakeUdpServer(0);
        badDelegatedServer.start();
        trackExtraFakeServer(badDelegatedServer);
    }

    @Override
    protected void configureResolverRouting(TestStepResolverFactory stepResolverFactory) {
        stepResolverFactory.setRoute(DEV_NS_IP, delegatedServer.getPort());
        stepResolverFactory.setRoute(DEV_HELPER_NS_IP, delegatedServer.getPort());
        stepResolverFactory.setRoute(BAD_NS_IP, badDelegatedServer.getPort());
    }

    @Test
    public void walksAllAncestorZoneCutsBeforeQueryingFinalName() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(fixture.key(DOMAIN, Type.A)));
    }

    @Test
    public void requiresIntermediateZoneCutsInsteadOfJumpingToFullNameNs() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();
        Message fullNameNsQuery = fixture.request(DOMAIN, Type.NS);
        fakeUpstreamServer.mockResponse(fullNameNsQuery, fixture.referralWithGlue(fullNameNsQuery, BAD_NS_HOST, BAD_NS_IP, TTL));

        Message badDomainAQuery = MessageHelper.buildARequest(DOMAIN);
        badDelegatedServer.mockResponse(badDomainAQuery, MessageHelper.buildAResponse(badDomainAQuery, BAD_FINAL_IP, TTL));

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(fixture.key(DOMAIN, Type.A)));
        assertReceivedQueries(badDelegatedServer, List.of());
    }

    @Test
    public void resolvesNoGlueIntermediateDelegationAfterFullWalk() throws IOException {
        FixtureHelper fixture = clearScenarioHistory();

        Message comNsQuery = fixture.request("com", Type.NS);
        fakeUpstreamServer.mockResponse(comNsQuery, fixture.referralWithGlue(comNsQuery, COM_NS_HOST, COM_NS_IP, TTL));

        Message exampleNsQuery = fixture.request(EXAMPLE_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(exampleNsQuery, fixture.referralWithGlue(exampleNsQuery, EXAMPLE_NS_HOST, EXAMPLE_NS_IP, TTL));

        Message devNsQuery = fixture.request(DEV_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(devNsQuery, MessageHelper.buildNsReferralResponse(devNsQuery, DEV_HELPER_NS_HOST, TTL));

        Message helperZoneNsQuery = fixture.request(DEV_HELPER_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(helperZoneNsQuery, fixture.referralWithGlue(helperZoneNsQuery, DEV_HELPER_NS_HOST, DEV_HELPER_NS_IP, TTL));

        Message helperAQuery = MessageHelper.buildARequest(DEV_HELPER_NS_HOST);
        delegatedServer.mockResponse(helperAQuery, MessageHelper.buildAResponse(helperAQuery, DEV_HELPER_NS_IP, TTL));

        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL));

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS),
                fixture.key("com", Type.NS),
                fixture.key(DEV_HELPER_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(
                fixture.key(DEV_HELPER_NS_HOST, Type.A),
                fixture.key(DOMAIN, Type.A)
        ));
    }

    @Test
    public void returnsNxdomainAfterDescendingThroughAllIntermediateZones() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();
        Message nxDomainAQuery = MessageHelper.buildARequest(NXDOMAIN);
        delegatedServer.mockResponse(nxDomainAQuery, MessageHelper.buildNxdomainResponseFrom(nxDomainAQuery, false));

        Message response = executeRequestOnOwnServer(NXDOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NXDOMAIN, response.getRcode());
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(fixture.key(NXDOMAIN, Type.A)));
    }

    @Test
    public void returnsNoDataAfterDescendingThroughAllIntermediateZones() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();
        Message noDataAQuery = fixture.request(NODATA, Type.A);
        delegatedServer.mockResponse(noDataAQuery, fixture.noDataResponse(noDataAQuery));

        Message response = executeRequestOnOwnServer(NODATA);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(0, response.getSection(org.xbill.DNS.Section.ANSWER).size());
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(fixture.key(NODATA, Type.A)));
    }

    @Test
    public void followsCnameAfterDescendingThroughAllIntermediateZones() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();
        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);
        delegatedServer.mockResponse(domainAQuery, fixture.cnameResponse(domainAQuery, CNAME_TARGET, TTL));

        Message cnameTargetAQuery = MessageHelper.buildARequest(CNAME_TARGET);
        delegatedServer.mockResponse(cnameTargetAQuery, MessageHelper.buildAResponse(cnameTargetAQuery, CNAME_FINAL_IP, TTL));

        Message response = executeRequestOnOwnServer(DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.NOERROR, response.getRcode());
        assertEquals(CNAME_FINAL_IP, MessageHelper.extractIpFromResponseMessage(response));
        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS),
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(
                fixture.key(DOMAIN, Type.A),
                fixture.key(CNAME_TARGET, Type.A)
        ));
    }

    @Test
    public void boundsReferralLoopAfterFullWalk() throws IOException {
        FixtureHelper fixture = loadMultiLevelDelegationScenario();
        Message loopAQuery = MessageHelper.buildARequest(LOOP_DOMAIN);
        delegatedServer.mockResponse(loopAQuery, fixture.referralWithGlue(loopAQuery, DEV_NS_HOST, DEV_NS_IP, TTL));

        Message response = executeRequestOnOwnServer(LOOP_DOMAIN);

        assertNotNull(response);
        assertEquals(Rcode.SERVFAIL, response.getRcode());

        assertReceivedQueries(fakeUpstreamServer, List.of(
                fixture.key("com", Type.NS),
                fixture.key(EXAMPLE_ZONE, Type.NS),
                fixture.key(DEV_ZONE, Type.NS)
        ));
        assertReceivedQueries(delegatedServer, List.of(fixture.key(LOOP_DOMAIN, Type.A)));
    }

    private FixtureHelper loadMultiLevelDelegationScenario() {
        FixtureHelper fixture = clearScenarioHistory();

        Message comNsQuery = fixture.request("com", Type.NS);
        fakeUpstreamServer.mockResponse(comNsQuery, fixture.referralWithGlue(comNsQuery, COM_NS_HOST, COM_NS_IP, TTL));

        Message exampleNsQuery = fixture.request(EXAMPLE_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(exampleNsQuery, fixture.referralWithGlue(exampleNsQuery, EXAMPLE_NS_HOST, EXAMPLE_NS_IP, TTL));

        Message devNsQuery = fixture.request(DEV_ZONE, Type.NS);
        fakeUpstreamServer.mockResponse(devNsQuery, fixture.referralWithGlue(devNsQuery, DEV_NS_HOST, DEV_NS_IP, TTL));

        Message domainAQuery = MessageHelper.buildARequest(DOMAIN);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, FINAL_IP, TTL));

        Message secondDomainAQuery = MessageHelper.buildARequest(SECOND_DOMAIN);
        delegatedServer.mockResponse(secondDomainAQuery, MessageHelper.buildAResponse(secondDomainAQuery, FINAL_IP, TTL));

        return fixture;
    }

    private FixtureHelper clearScenarioHistory() {
        fakeUpstreamServer.clearReceivedQueries();
        delegatedServer.clearReceivedQueries();
        badDelegatedServer.clearReceivedQueries();
        return new FixtureHelper(fakeUpstreamServer);
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

        private Message noDataResponse(Message request) {
            return buildNoDataResponse(request);
        }

        private Message cnameResponse(Message request, String target, long ttl) {
            return buildCnameResponse(request, target, ttl);
        }

        protected DnsQueryKey key(String qname, int qtype) {
            return super.key(qname, qtype);
        }
    }
}
