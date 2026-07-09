package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthoritativeSemanticFailoverFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer firstAuthoritativeServer;
    private final FakeServer secondAuthoritativeServer;
    private final FakeServer loopServer;

    public RecursiveAuthoritativeSemanticFailoverFixture(FakeServer fakeServer,
                                                         FakeServer firstAuthoritativeServer,
                                                         FakeServer secondAuthoritativeServer,
                                                         FakeServer loopServer) {
        super(fakeServer);
        this.firstAuthoritativeServer = firstAuthoritativeServer;
        this.secondAuthoritativeServer = secondAuthoritativeServer;
        this.loopServer = loopServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         String firstAuthNsHost,
                                         String firstAuthNsIp,
                                         String secondAuthNsHost,
                                         String secondAuthNsIp,
                                         String helperTargetHost,
                                         String helperZone,
                                         String helperZoneBootstrapHost,
                                         String helperZoneBootstrapIp,
                                         String loopNsAHost,
                                         String loopNsAIp,
                                         String loopNsBHost,
                                         String loopNsBIp,
                                         String finalIp,
                                         long ttl) {
        clearHistory();
        firstAuthoritativeServer.clearReceivedQueries();
        secondAuthoritativeServer.clearReceivedQueries();
        loopServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, ttl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGluesResponse(
                domainNsQuery,
                List.of(firstAuthNsHost, secondAuthNsHost),
                List.of(firstAuthNsIp, secondAuthNsIp),
                ttl
        ));

        Message netNsQuery = buildRequest("net", Type.NS);
        fakeServer.mockResponse(netNsQuery, buildNsReferralWithGlueResponse(netNsQuery, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl));

        Message helperZoneNsQuery = buildRequest(helperZone, Type.NS);
        fakeServer.mockResponse(helperZoneNsQuery, buildNsReferralWithGlueResponse(helperZoneNsQuery, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl));

        Message helperAQuery = buildRequest(helperTargetHost, Type.A);
        Message loopReferral = buildNsReferralWithGluesResponse(
                helperAQuery,
                List.of(loopNsAHost, loopNsBHost),
                List.of(loopNsAIp, loopNsBIp),
                ttl
        );
        loopServer.mockResponse(helperAQuery, loopReferral);

        Message loopNsAQuery = buildRequest(loopNsAHost, Type.A);
        loopServer.mockResponse(loopNsAQuery, buildNoDataResponse(loopNsAQuery));

        Message loopNsAAaaaQuery = buildRequest(loopNsAHost, Type.AAAA);
        loopServer.mockResponse(loopNsAAaaaQuery, buildNoDataResponse(loopNsAAaaaQuery));

        Message loopNsBQuery = buildRequest(loopNsBHost, Type.A);
        loopServer.mockResponse(loopNsBQuery, buildNoDataResponse(loopNsBQuery));

        Message loopNsBAaaaQuery = buildRequest(loopNsBHost, Type.AAAA);
        loopServer.mockResponse(loopNsBAaaaQuery, buildNoDataResponse(loopNsBAaaaQuery));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        firstAuthoritativeServer.mockResponse(domainAQuery, buildNsAuthorityWithoutGlue(domainAQuery, List.of(helperTargetHost), ttl));
        secondAuthoritativeServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, ttl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS)
                ),
                history(key(domain, Type.A)),
                history(
                        key(helperTargetHost, Type.A),
                        key(helperTargetHost, Type.A),
                        key(helperTargetHost, Type.A)
                )
        );
    }

    private Message buildNsAuthorityWithoutGlue(Message request, List<String> nsHosts, long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            Record question = request.getQuestion();
            if (question == null) {
                return response;
            }
            for (String nsHost : nsHosts) {
                String normalizedNsHost = nsHost.endsWith(".") ? nsHost : nsHost + ".";
                response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, Name.fromString(normalizedNsHost)), Section.AUTHORITY);
            }
            return response;
        } catch (TextParseException e) {
            fail("failed to create authority without glue: " + e.getMessage());
            return null;
        }
    }
}
