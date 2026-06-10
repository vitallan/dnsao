package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveRootHintFailoverFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveRootHintFailoverFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public RecursiveServerHistories load(String domain, String finalIp, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS)),
                history(key("com", Type.NS), key(domain, Type.NS), key(domain, Type.A)),
                history()
        );
    }
}
