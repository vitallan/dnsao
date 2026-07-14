package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.ReferralResult;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

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
        return ReferralResult.unusable();
    }
}
