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
public class RecursiveHelperReferralLoopDoesNotExpandFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;
    private final FakeServer loopServer;

    public RecursiveHelperReferralLoopDoesNotExpandFixture(FakeServer fakeServer, FakeServer delegatedServer, FakeServer loopServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
        this.loopServer = loopServer;
    }

    public RecursiveServerHistories loadSingleLoopingHelper(String domain,
                                                            String delegatedNameserverHost,
                                                            String delegatedNameserverIp,
                                                            String bootstrapNsHost,
                                                            String bootstrapNsIp,
                                                            String helperTargetHost,
                                                            String helperZone,
                                                            String helperZoneBootstrapHost,
                                                            String helperZoneBootstrapIp,
                                                            String loopNameserverA,
                                                            String loopNameserverAIp,
                                                            String loopNameserverB,
                                                            String loopNameserverBIp,
                                                            long ttl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        mockOriginalWalk(domain, delegatedNameserverHost, delegatedNameserverIp, bootstrapNsHost, bootstrapNsIp, ttl);
        mockHelperZoneBootstrap(helperZone, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl);
        mockLoopingHelper(helperTargetHost, loopNameserverA, loopNameserverAIp, loopNameserverB, loopNameserverBIp, ttl);
        mockLoopExpansionTargets(loopNameserverA, loopNameserverB, ttl);

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, buildNsAuthorityWithoutGlue(domainAQuery, List.of(helperTargetHost), ttl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("net", Type.NS),
                        key(helperZone, Type.NS)
                ),
                history(key(domain, Type.A)),
                history(
                        key(helperTargetHost, Type.A),
                        key(helperTargetHost, Type.A),
                        key(helperTargetHost, Type.A)
                )
        );
    }

    public RecursiveServerHistories loadFallsBackToSecondHelperAfterLoop(String domain,
                                                                         String delegatedNameserverHost,
                                                                         String delegatedNameserverIp,
                                                                         String resolvedAuthoritativeIp,
                                                                         String bootstrapNsHost,
                                                                         String bootstrapNsIp,
                                                                         String firstHelperTargetHost,
                                                                         String secondHelperTargetHost,
                                                                         String helperZone,
                                                                         String helperZoneBootstrapHost,
                                                                         String helperZoneBootstrapIp,
                                                                         String loopNameserverA,
                                                                         String loopNameserverAIp,
                                                                         String loopNameserverB,
                                                                         String loopNameserverBIp,
                                                                         String finalIp,
                                                                         long ttl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        mockOriginalWalk(domain, delegatedNameserverHost, delegatedNameserverIp, bootstrapNsHost, bootstrapNsIp, ttl);
        mockHelperZoneBootstrap(helperZone, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl);
        mockLoopingHelper(firstHelperTargetHost, loopNameserverA, loopNameserverAIp, loopNameserverB, loopNameserverBIp, ttl);
        mockLoopExpansionTargets(loopNameserverA, loopNameserverB, ttl);

        Message secondHelperAQuery = buildRequest(secondHelperTargetHost, Type.A);
        loopServer.mockResponse(secondHelperAQuery, MessageHelper.buildAResponse(secondHelperAQuery, resolvedAuthoritativeIp, ttl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponseChain(
                domainAQuery,
                buildNsAuthorityWithoutGlue(domainAQuery, List.of(firstHelperTargetHost, secondHelperTargetHost), ttl),
                MessageHelper.buildAResponse(domainAQuery, finalIp, ttl)
        );

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("net", Type.NS),
                        key(helperZone, Type.NS),
                        key("net", Type.NS),
                        key(helperZone, Type.NS)
                ),
                history(key(domain, Type.A), key(domain, Type.A)),
                history(
                        key(firstHelperTargetHost, Type.A),
                        key(firstHelperTargetHost, Type.A),
                        key(firstHelperTargetHost, Type.A),
                        key(secondHelperTargetHost, Type.A)
                )
        );
    }

    private void mockOriginalWalk(String domain,
                                  String delegatedNameserverHost,
                                  String delegatedNameserverIp,
                                  String bootstrapNsHost,
                                  String bootstrapNsIp,
                                  long ttl) {
        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, ttl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, delegatedNameserverHost, delegatedNameserverIp, ttl));
    }

    private void mockHelperZoneBootstrap(String helperZone,
                                         String helperZoneBootstrapHost,
                                         String helperZoneBootstrapIp,
                                         long ttl) {
        Message netNsQuery = buildRequest("net", Type.NS);
        fakeServer.mockResponse(netNsQuery, buildNsReferralWithGlueResponse(netNsQuery, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl));

        Message helperZoneNsQuery = buildRequest(helperZone, Type.NS);
        fakeServer.mockResponse(helperZoneNsQuery, buildNsReferralWithGlueResponse(helperZoneNsQuery, helperZoneBootstrapHost, helperZoneBootstrapIp, ttl));
    }

    private void mockLoopingHelper(String helperTargetHost,
                                   String loopNameserverA,
                                   String loopNameserverAIp,
                                   String loopNameserverB,
                                   String loopNameserverBIp,
                                   long ttl) {
        Message helperAQuery = buildRequest(helperTargetHost, Type.A);
        Message loopingReferral = buildNsReferralWithGluesResponse(
                helperAQuery,
                List.of(loopNameserverA, loopNameserverB),
                List.of(loopNameserverAIp, loopNameserverBIp),
                ttl
        );
        loopServer.mockResponse(helperAQuery, loopingReferral);

        Message helperAaaaQuery = buildRequest(helperTargetHost, Type.AAAA);
        loopServer.mockResponse(helperAaaaQuery, loopingReferral);
    }

    private void mockLoopExpansionTargets(String loopNameserverA, String loopNameserverB, long ttl) {
        Message loopARecordQuery = buildRequest(loopNameserverA, Type.A);
        loopServer.mockResponse(loopARecordQuery, buildNoDataResponse(loopARecordQuery));

        Message loopAaaaRecordQuery = buildRequest(loopNameserverA, Type.AAAA);
        loopServer.mockResponse(loopAaaaRecordQuery, buildNoDataResponse(loopAaaaRecordQuery));

        Message loopBRecordQuery = buildRequest(loopNameserverB, Type.A);
        loopServer.mockResponse(loopBRecordQuery, buildNoDataResponse(loopBRecordQuery));

        Message loopBaaaRecordQuery = buildRequest(loopNameserverB, Type.AAAA);
        loopServer.mockResponse(loopBaaaRecordQuery, buildNoDataResponse(loopBaaaRecordQuery));
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
            fail("failed to create NS authority without glue: " + e.getMessage());
            return null;
        }
    }
}
