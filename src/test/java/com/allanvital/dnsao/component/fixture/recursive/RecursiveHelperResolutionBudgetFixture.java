package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveHelperResolutionBudgetFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveHelperResolutionBudgetFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public RecursiveServerHistories load(String domain,
                                         String firstNameserverHost,
                                         String secondNameserverHost,
                                         String secondNameserverIp,
                                         long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, secondNameserverHost, secondNameserverIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, com.allanvital.dnsao.graph.bean.MessageHelper.buildNsReferralResponse(domainNsQuery, firstNameserverHost, referralTtl));

        Message firstNsNsQuery = buildRequest(firstNameserverHost, Type.NS);
        fakeServer.mockResponse(firstNsNsQuery, com.allanvital.dnsao.graph.bean.MessageHelper.buildNsReferralResponse(firstNsNsQuery, secondNameserverHost, referralTtl));

        String helperZone = firstNameserverHost.substring(firstNameserverHost.indexOf('.') + 1);
        Message helperZoneNsQuery = buildRequest(helperZone, Type.NS);
        fakeServer.mockResponse(helperZoneNsQuery, buildNsReferralWithGlueResponse(helperZoneNsQuery, secondNameserverHost, secondNameserverIp, referralTtl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("com", Type.NS),
                        key(helperZone, Type.NS),
                        key(firstNameserverHost, Type.A),
                        key("com", Type.NS),
                        key(helperZone, Type.NS),
                        key(firstNameserverHost, Type.AAAA)
                ),
                history(),
                history()
        );
    }
}
