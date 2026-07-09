package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

public class RecursiveCnameToDeepSubdomainFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer comServer;
    private final FakeServer deepServer;

    public RecursiveCnameToDeepSubdomainFixture(FakeServer fakeServer,
                                                FakeServer comServer,
                                                FakeServer deepServer) {
        super(fakeServer);
        this.comServer = comServer;
        this.deepServer = deepServer;
    }

    public RecursiveServerHistories load(String domain,
                                          String cnameTarget,
                                          String finalIp,
                                          String comServerIp,
                                          String deepServerIp,
                                          long ttl) {
        clearHistory();
        comServer.clearReceivedQueries();
        deepServer.clearReceivedQueries();

        // Root → com: NS → delegate to comServer
        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, "ns1.outbound.net", comServerIp, ttl));

        // comServer → domain.com NS: in-bailiwick NS → getReferralServers finds it
        Message domainNsQuery = buildRequest(domain, Type.NS);
        comServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, "ns1.domain.com", comServerIp, ttl));

        // comServer → domain.com A: CNAME to deep subdomain
        Message domainAQuery = MessageHelper.buildARequest(domain);
        comServer.mockResponse(domainAQuery, buildCnameResponse(domainAQuery, cnameTarget, ttl));

        // comServer → example.com NS → delegate to deepServer via a name outside example.com
        // so the current walk logic advances instead of pinning to comServer.
        Message exampleComFromComNsQuery = buildRequest("example.com", Type.NS);
        comServer.mockResponse(exampleComFromComNsQuery, buildNsReferralWithGlueResponse(exampleComFromComNsQuery, "ns1.com", deepServerIp, ttl));

        // Root → example.com NS → delegate to deepServer
        Message exampleComRootNsQuery = buildRequest("example.com", Type.NS);
        fakeServer.mockResponse(exampleComRootNsQuery, buildNsReferralWithGlueResponse(exampleComRootNsQuery, "ns1.deep.example.com", deepServerIp, ttl));

        // deepServer → example.com NS: in-bailiwick → shouldKeepCurrentServers keeps deepServer
        Message exampleComNsQuery = buildRequest("example.com", Type.NS);
        deepServer.mockResponse(exampleComNsQuery, buildNsReferralWithGlueResponse(exampleComNsQuery, "ns1.deep.example.com", deepServerIp, ttl));

        // deepServer → deep.example.com NS: in-bailiwick → shouldKeepCurrentServers keeps deepServer
        Message deepNsQuery = buildRequest("deep.example.com", Type.NS);
        deepServer.mockResponse(deepNsQuery, buildNsReferralWithGlueResponse(deepNsQuery, "ns1.deep.example.com", deepServerIp, ttl));

        // deepServer → zone.deep.example.com NS: non-zone, NOERROR/empty (THE SPURIOUS NAME)
        Message spuriousNsQuery = buildRequest("zone.deep.example.com", Type.NS);
        deepServer.mockResponse(spuriousNsQuery, buildNoDataResponse(spuriousNsQuery));

        // deepServer → a.zone.deep.example.com NS: non-zone, NOERROR/empty (last step)
        Message cnameTargetNsQuery = buildRequest(cnameTarget, Type.NS);
        deepServer.mockResponse(cnameTargetNsQuery, buildNoDataResponse(cnameTargetNsQuery));

        // deepServer → a.zone.deep.example.com A: final answer
        Message cnameTargetAQuery = MessageHelper.buildARequest(cnameTarget);
        deepServer.mockResponse(cnameTargetAQuery, MessageHelper.buildAResponse(cnameTargetAQuery, finalIp, ttl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key("com", Type.NS)
                ),
                history(
                        key(domain, Type.NS),
                        key(domain, Type.A),
                        key("example.com", Type.NS)
                ),
                history(
                        key("deep.example.com", Type.NS),
                        key("zone.deep.example.com", Type.NS),
                        key(cnameTarget, Type.A)
                )
        );
    }
}
