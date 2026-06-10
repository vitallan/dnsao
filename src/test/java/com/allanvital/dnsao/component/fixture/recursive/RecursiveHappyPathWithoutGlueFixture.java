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
public class RecursiveHappyPathWithoutGlueFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveHappyPathWithoutGlueFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String nsHost, String nsIp, String finalIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, MessageHelper.buildNsReferralResponse(domainNsQuery, nsHost, referralTtl));

        Message nsNsQuery = buildRequest(nsHost, Type.NS);
        fakeServer.mockResponse(nsNsQuery, buildNsReferralWithGlueResponse(nsNsQuery, nsHost, nsIp, referralTtl));

        Message nsAQuery = MessageHelper.buildARequest(nsHost);
        fakeServer.mockResponse(nsAQuery, MessageHelper.buildAResponse(nsAQuery, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key("com", Type.NS),
                key(nsHost, Type.NS),
                key(nsHost, Type.A),
                key(domain, Type.A)
        );
    }
}
