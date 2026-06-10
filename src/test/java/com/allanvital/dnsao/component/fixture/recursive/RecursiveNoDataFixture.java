package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveNoDataFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveNoDataFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = buildRequest(domain, Type.A);
        fakeServer.mockResponse(domainAQuery, buildNoDataResponse(domainAQuery));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A)
        );
    }
}
