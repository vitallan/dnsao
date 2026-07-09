package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.RRSIGRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.time.Instant;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveClientFacingPositiveAnswerFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveClientFacingPositiveAnswerFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories loadAuthoritativeStylePositiveAnswer(String domain,
                                                                         String delegatedNameserverHost,
                                                                         String delegatedNameserverIp,
                                                                         String bootstrapNsHost,
                                                                         String bootstrapNsIp,
                                                                         String finalIp,
                                                                         long ttl,
                                                                         boolean includeDnssecAnswerRecords) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, ttl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildNsReferralWithGlueResponse(domainNsQuery, delegatedNameserverHost, delegatedNameserverIp, ttl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, buildAuthoritativePositiveAnswer(domainAQuery, delegatedNameserverHost, finalIp, ttl, includeDnssecAnswerRecords));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A)),
                history()
        );
    }

    private Message buildAuthoritativePositiveAnswer(Message request,
                                                     String delegatedNameserverHost,
                                                     String finalIp,
                                                     long ttl,
                                                     boolean includeDnssecAnswerRecords) {
        try {
            Message response = buildNoDataResponse(request);
            response.getHeader().setFlag(Flags.AA);

            Record question = request.getQuestion();
            if (question == null) {
                return response;
            }

            Name qname = question.getName();
            response.addRecord(new ARecord(qname, DClass.IN, ttl, InetAddress.getByName(finalIp)), Section.ANSWER);
            if (includeDnssecAnswerRecords) {
                response.addRecord(buildRrsigRecord(qname, Type.A, ttl), Section.ANSWER);
            }

            Name nsName = Name.fromString(delegatedNameserverHost.endsWith(".") ? delegatedNameserverHost : delegatedNameserverHost + ".");
            response.addRecord(new NSRecord(qname, DClass.IN, ttl * 96, nsName), Section.AUTHORITY);
            if (includeDnssecAnswerRecords) {
                response.addRecord(buildRrsigRecord(qname, Type.NS, ttl * 96), Section.AUTHORITY);
            }
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("failed to build authoritative positive answer", e);
        }
    }

    private RRSIGRecord buildRrsigRecord(Name owner, int typeCovered, long ttl) throws TextParseException {
        return new RRSIGRecord(
                owner,
                DClass.IN,
                ttl,
                typeCovered,
                8,
                ttl,
                Instant.parse("2026-08-03T03:31:08Z"),
                Instant.parse("2026-06-24T03:05:54Z"),
                12345,
                owner,
                new byte[]{1, 2, 3, 4}
        );
    }
}
