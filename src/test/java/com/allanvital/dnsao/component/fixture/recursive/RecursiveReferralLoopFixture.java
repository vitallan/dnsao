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
public class RecursiveReferralLoopFixture extends AbstractRecursiveScenarioFixture {

    public RecursiveReferralLoopFixture(FakeServer fakeServer) {
        super(fakeServer);
    }

    public List<DnsQueryKey> load(String domain, String nsHost, String nsIp, long referralTtl) {
        clearHistory();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, nsHost, nsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, nsHost, nsIp, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        fakeServer.mockResponse(domainAQuery, buildNsReferralWithGlueResponse(domainAQuery, nsHost, nsIp, referralTtl));

        return mergeHistories(
                history(key("com", Type.NS), key(domain.substring(domain.indexOf('.') + 1), Type.NS)),
                history(key(domain, Type.A)),
                history(
                        key("com", Type.NS),
                        key(domain.substring(domain.indexOf('.') + 1), Type.NS),
                        key(domain, Type.NS),
                        key(nsHost, Type.A)
                ),
                history(
                        key("com", Type.NS),
                        key(domain.substring(domain.indexOf('.') + 1), Type.NS),
                        key(domain, Type.NS),
                        key(nsHost, Type.AAAA)
                )
        );
    }
}
