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
public class RecursiveEmptyReferralFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveEmptyReferralFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String emptyNameserverHost, String bootstrapNsHost, String bootstrapNsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, MessageHelper.buildNsReferralResponse(domainNsQuery, emptyNameserverHost, referralTtl));

        Message emptyNsNsQuery = buildRequest(emptyNameserverHost, Type.NS);
        fakeServer.mockResponse(emptyNsNsQuery, buildNoDataResponse(emptyNsNsQuery));

        String emptyZone = emptyNameserverHost.substring(emptyNameserverHost.indexOf('.') + 1);
        Message emptyZoneNsQuery = buildRequest(emptyZone, Type.NS);
        fakeServer.mockResponse(emptyZoneNsQuery, buildNoDataResponse(emptyZoneNsQuery));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key("com", Type.NS),
                key(emptyZone, Type.NS),
                key(emptyNameserverHost, Type.A),
                key("com", Type.NS),
                key(emptyZone, Type.NS),
                key(emptyNameserverHost, Type.AAAA),
                key(domain, Type.A)
        );
    }
}
