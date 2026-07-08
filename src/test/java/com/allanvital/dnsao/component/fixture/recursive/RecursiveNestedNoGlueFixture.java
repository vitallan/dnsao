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
public class RecursiveNestedNoGlueFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveNestedNoGlueFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain,
                                  String firstNameserverHost,
                                  String secondNameserverHost,
                                  String secondNameserverIp,
                                  String firstNameserverIp,
                                  String finalIp,
                                  long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, secondNameserverHost, secondNameserverIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, MessageHelper.buildNsReferralResponse(domainNsQuery, firstNameserverHost, referralTtl));

        Message firstNsNsQuery = buildRequest(firstNameserverHost, Type.NS);
        fakeServer.mockResponse(firstNsNsQuery, MessageHelper.buildNsReferralResponse(firstNsNsQuery, secondNameserverHost, referralTtl));

        Message helperZoneNsQuery = buildRequest(firstNameserverHost.substring(firstNameserverHost.indexOf('.') + 1), Type.NS);
        fakeServer.mockResponse(helperZoneNsQuery, buildNsReferralWithGlueResponse(helperZoneNsQuery, secondNameserverHost, secondNameserverIp, referralTtl));

        Message secondNsNsQuery = buildRequest(secondNameserverHost, Type.NS);
        fakeServer.mockResponse(secondNsNsQuery, buildNsReferralWithGlueResponse(secondNsNsQuery, secondNameserverHost, secondNameserverIp, referralTtl));

        Message secondNsAQuery = MessageHelper.buildARequest(secondNameserverHost);
        fakeServer.mockResponse(secondNsAQuery, MessageHelper.buildAResponse(secondNsAQuery, secondNameserverIp, referralTtl));

        Message firstNsAQuery = MessageHelper.buildARequest(firstNameserverHost);
        fakeServer.mockResponse(firstNsAQuery, MessageHelper.buildAResponse(firstNsAQuery, firstNameserverIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return history(
                key("com", Type.NS),
                key(domain, Type.NS),
                key("com", Type.NS),
                key(firstNameserverHost.substring(firstNameserverHost.indexOf('.') + 1), Type.NS),
                key(firstNameserverHost, Type.A),
                key(domain, Type.A)
        );
    }
}
