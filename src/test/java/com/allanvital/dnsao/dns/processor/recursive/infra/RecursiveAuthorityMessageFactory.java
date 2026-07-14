package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.graph.bean.MessageHelper;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthorityMessageFactory {

    public Message buildReferral(Message query, ReferralAnswer referralAnswer) {
        try {
            Message response = new Message(query.getHeader().getID());
            response.getHeader().setFlag(Flags.QR);
            response.addRecord(query.getQuestion(), Section.QUESTION);

            Name zone = Name.fromString(referralAnswer.getDelegatedZone());
            int dclass = query.getQuestion().getDClass();

            for (String nameserver : referralAnswer.getNameservers()) {
                response.addRecord(new NSRecord(zone, dclass, referralAnswer.getTtl(), Name.fromString(nameserver)), Section.AUTHORITY);
            }

            for (GlueRecord glueRecord : referralAnswer.getGlueRecords()) {
                Name ownerName = Name.fromString(glueRecord.getOwnerName());
                InetAddress address = InetAddress.getByName(glueRecord.getIp());
                response.addRecord(new ARecord(ownerName, dclass, referralAnswer.getTtl(), address), Section.ADDITIONAL);
            }

            return response;
        } catch (IOException e) {
            throw new IllegalStateException("failed to build referral response", e);
        }
    }

    public Message buildPositiveAnswer(Message query, PositiveAnswer positiveAnswer) {
        if (positiveAnswer.getType() == Type.A) {
            return MessageHelper.buildAResponse(query, positiveAnswer.getValue(), positiveAnswer.getTtl());
        }
        throw new IllegalArgumentException("unsupported positive answer type for scenario: " + Type.string(positiveAnswer.getType()));
    }
}
