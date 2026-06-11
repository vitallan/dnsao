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
public class RecursiveCnameLoopFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveCnameLoopFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String aliasTarget, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, buildCnameResponse(domainAQuery, aliasTarget, referralTtl));

        Message aliasNsQuery = buildRequest(aliasTarget, Type.NS);
        fakeServer.mockResponse(aliasNsQuery, buildNsReferralWithGlueResponse(aliasNsQuery, nsHost, nsIp, referralTtl));

        Message aliasAQuery = MessageHelper.buildARequest(aliasTarget);
        fakeServer.mockResponse(aliasAQuery, buildCnameResponse(aliasAQuery, domain, referralTtl));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A),
                key("com", Type.NS),
                key(aliasTarget, Type.NS),
                key(aliasTarget, Type.A)
        );
    }
}
