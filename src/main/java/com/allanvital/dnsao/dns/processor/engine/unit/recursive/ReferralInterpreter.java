package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.DelegationPoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.ReferralResult;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ReferralInterpreter {

    public ReferralResult interpret(Message authorityResponse) {
        if (authorityResponse == null) {
            return ReferralResult.unusable();
        }
        if (authorityResponse.getRcode() == Rcode.NOERROR && !authorityResponse.getSection(Section.ANSWER).isEmpty()) {
            return ReferralResult.finalAnswer(authorityResponse);
        }

        List<Record> authority = authorityResponse.getSection(Section.AUTHORITY);
        List<Record> additional = authorityResponse.getSection(Section.ADDITIONAL);
        if (authority == null || authority.isEmpty()) {
            return ReferralResult.unusable();
        }

        String delegatedZone = null;
        List<String> nameservers = new ArrayList<>();
        for (Record record : authority) {
            if (record instanceof NSRecord nsRecord) {
                delegatedZone = nsRecord.getName().toString();
                nameservers.add(nsRecord.getTarget().toString());
            }
        }
        if (delegatedZone == null || nameservers.isEmpty()) {
            return ReferralResult.unusable();
        }

        List<AuthorityEndpoint> authorityEndpoints = new ArrayList<>();
        for (Record record : additional) {
            if (!(record instanceof ARecord aRecord)) {
                continue;
            }
            String ownerName = aRecord.getName().toString();
            if (!nameservers.contains(ownerName)) {
                continue;
            }
            authorityEndpoints.add(new AuthorityEndpoint(ownerName, aRecord.getAddress(), 53));
        }
        return ReferralResult.referral(new DelegationPoint(delegatedZone, nameservers, authorityEndpoints));
    }
}
