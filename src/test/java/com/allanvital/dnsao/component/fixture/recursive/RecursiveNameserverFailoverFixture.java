package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.fake.FakeServer;
import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveNameserverFailoverFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer workingNameserverServer;

    public RecursiveNameserverFailoverFixture(FakeServer fakeServer, FakeServer workingNameserverServer) {
        super(fakeServer);
        this.workingNameserverServer = workingNameserverServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String firstNsHost,
                                         String firstNsIp,
                                         String secondNsHost,
                                         String secondNsIp,
                                         String finalIp,
                                         long referralTtl) {
        clearHistory();
        workingNameserverServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, firstNsHost, firstNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGluesResponse(domainNsQuery, List.of(firstNsHost, secondNsHost), List.of(firstNsIp, secondNsIp), referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        workingNameserverServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A)),
                history(key(domain, Type.A))
        );
    }
}
