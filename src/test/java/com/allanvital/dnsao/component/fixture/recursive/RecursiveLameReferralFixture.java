package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveLameReferralFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveLameReferralFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String delegatedNameserverHost,
                                         String delegatedNameserverIp,
                                         String lameNameserverHost,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         long referralTtl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, delegatedNameserverHost, delegatedNameserverIp, referralTtl));

        Message lameNsNsQuery = buildRequest(lameNameserverHost, Type.NS);
        fakeServer.mockResponse(lameNsNsQuery, buildNoDataResponse(lameNsNsQuery));

        String lameZone = lameNameserverHost.substring(lameNameserverHost.indexOf('.') + 1);
        Message lameZoneNsQuery = buildRequest(lameZone, Type.NS);
        fakeServer.mockResponse(lameZoneNsQuery, buildNoDataResponse(lameZoneNsQuery));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildNsReferralResponse(domainAQuery, lameNameserverHost, referralTtl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("com", Type.NS),
                        key(lameZone, Type.NS),
                        key(lameNameserverHost, Type.A),
                        key("com", Type.NS),
                        key(lameZone, Type.NS),
                        key(lameNameserverHost, Type.AAAA)
                ),
                history(key(domain, Type.A)),
                history()
        );
    }
}
