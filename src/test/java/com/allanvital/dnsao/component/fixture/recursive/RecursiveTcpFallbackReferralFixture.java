package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveTcpFallbackReferralFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer transportServer;

    public RecursiveTcpFallbackReferralFixture(FakeServer fakeServer, FakeServer transportServer) {
        super(fakeServer);
        this.transportServer = transportServer;
    }

    public RecursiveTransportServerHistories load(String domain,
                                                  String bootstrapNsHost,
                                                  String bootstrapNsIpv4,
                                                  String transportNameserverHost,
                                                  String transportNameserverIp,
                                                  String finalIp,
                                                  long referralTtl) {
        clearHistory();
        transportServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, transportNameserverHost, transportNameserverIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        transportServer.mockResponseChain(
                domainNsQuery,
                MessageHelper.buildTruncatedResponse(domainNsQuery),
                buildNsReferralWithGlueResponse(domainNsQuery, transportNameserverHost, transportNameserverIp, referralTtl)
        );

        Message domainAQuery = MessageHelper.buildARequest(domain);
        transportServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveTransportServerHistories(
                history(),
                history(key("com", Type.NS), key(domain, Type.NS), key(domain, Type.A)),
                history(key(domain, Type.NS))
        );
    }
}
