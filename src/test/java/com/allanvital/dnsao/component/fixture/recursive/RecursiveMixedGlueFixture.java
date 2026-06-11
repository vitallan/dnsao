package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import com.allanvital.dnsao.graph.fake.FakeServer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveMixedGlueFixture extends AbstractRecursiveScenarioFixture {

    private final FakeServer delegatedServer;

    public RecursiveMixedGlueFixture(FakeServer fakeServer, FakeServer delegatedServer) {
        super(fakeServer);
        this.delegatedServer = delegatedServer;
    }

    public RecursiveServerHistories load(String domain,
                                         String gluedNameserverHost,
                                         String gluedNameserverIp,
                                         String glueLessNameserverHost,
                                         String bootstrapNsHost,
                                         String bootstrapNsIp,
                                         String finalIp,
                                         long referralTtl) {
        clearHistory();
        delegatedServer.clearReceivedQueries();

        Message comNsQuery = buildRequest("com", Type.NS);
        fakeServer.mockResponse(comNsQuery, buildNsReferralWithGlueResponse(comNsQuery, bootstrapNsHost, bootstrapNsIp, referralTtl));

        Message domainNsQuery = buildRequest(domain, Type.NS);
        fakeServer.mockResponse(domainNsQuery, buildMixedGlueReferral(domainNsQuery, gluedNameserverHost, gluedNameserverIp, glueLessNameserverHost, referralTtl));

        Message domainAQuery = MessageHelper.buildARequest(domain);
        delegatedServer.mockResponse(domainAQuery, MessageHelper.buildAResponse(domainAQuery, finalIp, referralTtl));

        return new RecursiveServerHistories(
                history(key("com", Type.NS), key(domain, Type.NS)),
                history(key(domain, Type.A)),
                history()
        );
    }

    private Message buildMixedGlueReferral(Message request,
                                           String gluedNameserverHost,
                                           String gluedNameserverIp,
                                           String glueLessNameserverHost,
                                           long ttl) {
        try {
            Message response = buildNoDataResponse(request);
            Record question = request.getQuestion();
            if (question == null) {
                return response;
            }

            Name gluedName = Name.fromString(gluedNameserverHost.endsWith(".") ? gluedNameserverHost : gluedNameserverHost + ".");
            Name glueLessName = Name.fromString(glueLessNameserverHost.endsWith(".") ? glueLessNameserverHost : glueLessNameserverHost + ".");

            response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, gluedName), Section.AUTHORITY);
            response.addRecord(new NSRecord(question.getName(), question.getDClass(), ttl, glueLessName), Section.AUTHORITY);
            response.addRecord(new ARecord(gluedName, DClass.IN, ttl, java.net.InetAddress.getByName(gluedNameserverIp)), Section.ADDITIONAL);
            return response;
        } catch (IOException e) {
            fail("failed to create mixed-glue referral: " + e.getMessage());
            return null;
        }
    }
}
