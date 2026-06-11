package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveDoDowngradeFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer transportServer;

    public RecursiveDoDowngradeFixture(FakeServer fakeServer, FakeServer transportServer) {
        super(fakeServer);
        this.transportServer = transportServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String bootstrapNsHost,
                                         String bootstrapNsIpv4,
                                         String transportNameserverHost,
                                         String transportNameserverIp,
                                         String finalIp,
                                         long referralTtl,
                                         int downgradeTriggerRcode) {
        clearHistory();
        transportServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIpv4, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, transportNameserverHost, transportNameserverIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        transportServer.mockResponseChain(
                domainAQuery,
                MessageHelper.buildResponseWithRcode(domainAQuery, downgradeTriggerRcode),
                MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl)
        );

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A), key(domain, Type.A)),
                history()
        );
    }
}
