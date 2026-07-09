package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
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
public class RecursiveAuthoritativeNsInAuthorityFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveAuthoritativeNsInAuthorityFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String delegatedNameserverHost,
                                         String delegatedNameserverIp,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         String helperZoneNsHost,
                                         String helperZoneNsIp,
                                         String authorityNsHost,
                                         long referralTtl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, delegatedNameserverHost, delegatedNameserverIp, referralTtl));

        Message netNsQuery = buildRequest("net", Type.NS);
        fakeServer.mockResponse(netNsQuery, buildNsReferralWithGlueResponse(netNsQuery, helperZoneNsHost, helperZoneNsIp, referralTtl));

        Message helperZoneNsQuery = buildRequest(authorityNsHost.substring(authorityNsHost.indexOf('.') + 1), Type.NS);
        fakeServer.mockResponse(helperZoneNsQuery, buildNsReferralWithGlueResponse(helperZoneNsQuery, helperZoneNsHost, helperZoneNsIp, referralTtl));

        Message helperAQuery = buildRequest(authorityNsHost, Type.A);
        fakeServer.mockResponse(helperAQuery, buildNoDataResponse(helperAQuery));

        Message helperAaaaQuery = buildRequest(authorityNsHost, Type.AAAA);
        fakeServer.mockResponse(helperAaaaQuery, buildNoDataResponse(helperAaaaQuery));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, buildAuthoritativeEmptyAnswerWithNsAuthority(domainAQuery, authorityNsHost, referralTtl));

        return new RecursiveServerHistories(
                history(
                        key("com", Type.NS),
                        key(domain, Type.NS),
                        key("com", Type.NS),
                        key(authorityNsHost.substring(authorityNsHost.indexOf('.') + 1), Type.NS),
                        key(authorityNsHost, Type.A),
                        key("com", Type.NS),
                        key(authorityNsHost.substring(authorityNsHost.indexOf('.') + 1), Type.NS),
                        key(authorityNsHost, Type.AAAA)
                ),
                history(key(domain, Type.A)),
                history()
        );
    }

    private Message buildAuthoritativeEmptyAnswerWithNsAuthority(Message request, String authorityNsHost, long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            response.getHeader().setFlag(Flags.AA);
            Record question = request.getQuestion();
            if (question != null) {
                String normalizedAuthorityNsHost = authorityNsHost.endsWith(".") ? authorityNsHost : authorityNsHost + ".";
                response.addRecord(new NSRecord(question.getName(), DClass.IN, ttl, Name.fromString(normalizedAuthorityNsHost)), Section.AUTHORITY);
            }
            return response;
        } catch (TextParseException e) {
            fail("failed to build authoritative empty answer with NS authority: " + e.getMessage());
            return null;
        }
    }
}
