package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthoritativeAnswerAfterDescentFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveAuthoritativeAnswerAfterDescentFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String delegatedNameserverHost,
                                         String delegatedNameserverIp,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         String finalIp,
                                         long referralTtl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, delegatedNameserverHost, delegatedNameserverIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A)),
                history()
        );
    }
}
