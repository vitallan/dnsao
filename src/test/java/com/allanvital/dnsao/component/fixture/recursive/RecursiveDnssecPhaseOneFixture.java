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
public class RecursiveDnssecPhaseOneFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveDnssecPhaseOneFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> loadHappyPathWithGlue(String domain, String finalIp, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A)
        );
    }

    public List<DnsQueryKey> loadAuthenticatedAnswer(String domain, String finalIp, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl, true));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key(domain, Type.A)
        );
    }

    public List<DnsQueryKey> loadAuthenticatedNxDomain(String domain, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        String parentZone = domain.substring(domain.indexOf('.') + 1);
        Message parentNsQuery = buildRequest(parentZone, Type.NS);
        fakeServer.mockResponse(parentNsQuery, buildNsReferralWithGlueResponse(parentNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildNxdomainResponseFrom(domainAQuery, true));

        return history(
                key("com", Type.NS),
                key(parentZone, Type.NS),
                key(domain, Type.A)
        );
    }
}
