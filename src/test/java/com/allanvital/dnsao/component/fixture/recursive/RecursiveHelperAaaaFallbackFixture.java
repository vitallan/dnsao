package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHelperAaaaFallbackFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveHelperAaaaFallbackFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String helperNameserverHost,
                                         String helperNameserverIpv6,
                                         String bootstrapNsHost,
                                         String bootstrapNsIpv4,
                                         String finalIp,
                                         long referralTtl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIpv4, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, MessageHelper.buildNsReferralResponse(domainNsQuery, helperNameserverHost, referralTtl));

        Message helperNsQuery = buildRequest(helperNameserverHost, Type.NS);
        fakeServer.mockResponse(helperNsQuery, buildNsReferralWithGlueResponse(helperNsQuery, bootstrapNsHost, bootstrapNsIpv4, referralTtl));

        Message helperAQuery = MessageHelper.buildARequest(helperNameserverHost);
        fakeServer.mockResponse(helperAQuery, buildNoDataResponse(helperAQuery));

        Message helperAaaaQuery = buildRequest(helperNameserverHost, Type.AAAA);
        fakeServer.mockResponse(helperAaaaQuery, MessageHelper.buildAaaaResponse(helperAaaaQuery, helperNameserverIpv6, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("com", Type.NS),
                        key(helperNameserverHost, Type.NS),
                        key(helperNameserverHost, Type.A),
                        key("com", Type.NS),
                        key(helperNameserverHost, Type.NS),
                        key(helperNameserverHost, Type.AAAA)
                ),
                history(key(domain, Type.A)),
                history()
        );
    }
}
