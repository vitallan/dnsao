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
public class RecursiveCnameChainFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveCnameChainFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String intermediateTarget, String finalTarget, String finalIp, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, buildCnameResponse(domainAQuery, intermediateTarget, referralTtl));

        Message intermediateNsQuery = buildRequest(intermediateTarget, Type.NS);
        fakeServer.mockResponse(intermediateNsQuery, buildNsReferralWithGlueResponse(intermediateNsQuery, nsHost, nsIp, referralTtl));

        Message intermediateAQuery = MessageHelper.buildARequest(intermediateTarget);
        fakeServer.mockResponse(intermediateAQuery, buildCnameResponse(intermediateAQuery, finalTarget, referralTtl));

        Message finalNsQuery = buildRequest(finalTarget, Type.NS);
        fakeServer.mockResponse(finalNsQuery, buildNsReferralWithGlueResponse(finalNsQuery, nsHost, nsIp, referralTtl));

        Message finalAQuery = MessageHelper.buildARequest(finalTarget);
        fakeServer.mockResponse(finalAQuery, MessageHelper.buildAResponse(finalAQuery, finalIp, referralTtl));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A),
                key("com", Type.NS),
                key(intermediateTarget, Type.NS),
                key(intermediateTarget, Type.A),
                key("com", Type.NS),
                key(finalTarget, Type.NS),
                key(finalTarget, Type.A)
        );
    }
}
