package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface EngineUnit {

    long DEFAULT_LOCAL_TTL = 60L;

    DnsQueryResponse process(DnsQueryRequest dnsQueryRequest);

    default Name getQuestionName(Message message) {
        Record question = message.getQuestion();
        return question.getName();
    }

}
