package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveWallClockBudgetFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delayedRootServer;
    private final FakeServer authoritativeServer;

    public RecursiveWallClockBudgetFixture(FakeServer delayedRootServer, FakeServer authoritativeServer) {
        super(delayedRootServer);
        this.delayedRootServer = delayedRootServer;
        this.authoritativeServer = authoritativeServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         String authoritativeNsHost,
                                         String authoritativeNsIp,
                                         String finalIp,
                                         long referralTtl) {
        delayedRootServer.clearReceivedQueries();
        authoritativeServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        delayedRootServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        delayedRootServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, authoritativeNsHost, authoritativeNsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        authoritativeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(),
                history()
        );
    }
}
