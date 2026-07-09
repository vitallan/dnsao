package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class StepResponseReferralClassificationTest {

    private static final String DOMAIN = "authoritative-ns-authority.com";
    private static final String AUTHORITY_NS_HOST = "ns2.helper.net.";
    private static final String REFERRAL_NS_HOST = "ns1.authoritative-ns-authority.com.";
    private static final String REFERRAL_NS_IP = "127.0.0.81";
    private static final long TTL = 300;

    @Test
    public void authoritativeNoerrorWithNsAuthorityIsNotTreatedAsReferral() throws Exception {
        Message request = MessageHelper.buildARequest(DOMAIN);
        Message response = authoritativeEmptyAnswerWithNsAuthority(request, AUTHORITY_NS_HOST);

        StepResponse stepResponse = new StepResponse(response);

        assertTrue(stepResponse.getReferralServers().isEmpty());
        assertTrue(stepResponse.getNSTargets().isEmpty());
        assertTrue(stepResponse.isNoDataFor(Name.fromString(DOMAIN + "."), Type.A));
    }

    @Test
    public void nonAuthoritativeNsAuthorityStillBehavesLikeReferral() throws Exception {
        Message request = MessageHelper.buildARequest(DOMAIN);
        Message response = nonAuthoritativeReferralWithGlue(request, REFERRAL_NS_HOST, REFERRAL_NS_IP);

        StepResponse stepResponse = new StepResponse(response);

        assertEquals(List.of(Name.fromString(REFERRAL_NS_HOST)), stepResponse.getNSTargets());
        assertEquals(1, stepResponse.getReferralServers().size());
        assertEquals(REFERRAL_NS_IP, stepResponse.getReferralServers().get(0).ip());
    }

    private Message authoritativeEmptyAnswerWithNsAuthority(Message request, String authorityNsHost) throws Exception {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setOpcode(request.getHeader().getOpcode());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setFlag(Flags.AA);
        Record question = request.getQuestion();
        response.addRecord(question, Section.QUESTION);
        response.addRecord(new NSRecord(question.getName(), DClass.IN, TTL, Name.fromString(authorityNsHost)), Section.AUTHORITY);
        return response;
    }

    private Message nonAuthoritativeReferralWithGlue(Message request, String nsHost, String nsIp) throws Exception {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setOpcode(request.getHeader().getOpcode());
        response.getHeader().setFlag(Flags.QR);
        Record question = request.getQuestion();
        response.addRecord(question, Section.QUESTION);
        Name nsName = Name.fromString(nsHost);
        response.addRecord(new NSRecord(question.getName(), DClass.IN, TTL, nsName), Section.AUTHORITY);
        response.addRecord(new ARecord(nsName, DClass.IN, TTL, InetAddress.getByName(nsIp)), Section.ADDITIONAL);
        return response;
    }
}
