package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveIntermediateWalkAdvancesToDelegatedServersFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer tldServer;
    private final FakeServer authoritativeServer;

    public RecursiveIntermediateWalkAdvancesToDelegatedServersFixture(FakeServer rootServer,
                                                                      FakeServer tldServer,
                                                                      FakeServer authoritativeServer) {
        super(rootServer);
        this.tldServer = tldServer;
        this.authoritativeServer = authoritativeServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String tldNameserverHost,
                                         String tldNameserverIp,
                                         String authoritativeNameserverHost,
                                         String authoritativeNameserverIp,
                                         String finalIp,
                                         long ttl) {
        clearHistory();
        tldServer.clearReceivedQueries();
        authoritativeServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, tldNameserverHost, tldNameserverIp, ttl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        tldServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, authoritativeNameserverHost, authoritativeNameserverIp, ttl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        authoritativeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, ttl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS)),
                history(key(domain, Type.NS)),
                history(key(domain, Type.A))
        );
    }
}
