package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.block.BlockDecider;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.clock.Clock;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.allanvital.dnsao.dns.remote.DnsUtils.baseResponse;
import static com.allanvital.dnsao.dns.remote.DnsUtils.formErr;
import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.BLOCKED;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockUnit implements EngineUnit {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    private final BlockDecider blockDecider;

    public BlockUnit(BlockDecider blockDecider) {
        this.blockDecider = blockDecider;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        Message request = dnsQueryRequest.getRequest();
        InetAddress client = dnsQueryRequest.getClientAddress();
        Name question = getQuestionName(request);
        if (blockDecider == null || !blockDecider.isBlocked(client, question)) {
            return null;
        }
        try {
            Message response = buildBlocked(request);
            return new DnsQueryResponse(dnsQueryRequest, response);
        } catch (UnknownHostException | TextParseException e) {
            log.error("it was not possible to build a blocked response: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return BLOCKED;
    }

    public static Message buildBlocked(Message query) throws UnknownHostException, TextParseException {
        Record question = query.getQuestion();
        if (question == null) {
            return formErr(query);
        }

        Name qname = question.getName();
        int qtype = question.getType();
        int qclass = question.getDClass();

        Message response = baseResponse(query, question);
        response.getHeader().setRcode(Rcode.NOERROR);

        if (qtype == Type.A) {
            ARecord arec = new ARecord(qname, qclass, DEFAULT_LOCAL_TTL, InetAddress.getByName("0.0.0.0"));
            response.addRecord(arec, Section.ANSWER);
        } else if (qtype == Type.AAAA) {
            AAAARecord aaaa = new AAAARecord(qname, qclass, DEFAULT_LOCAL_TTL, InetAddress.getByName("::"));
            response.addRecord(aaaa, Section.ANSWER);
        } else {
            response.addRecord(makeSoaFor(qclass), Section.AUTHORITY);
        }

        return response;
    }

    private static SOARecord makeSoaFor(int qclass) throws TextParseException {
        String defaultZone = "block.local.";
        long refresh = 3600L;
        long retry = 300L;
        long expire = 86400L;
        long serial = Clock.currentTimeInMillis() / 1000L;

        Name soaZone = Name.fromString(defaultZone);
        Name mname = Name.fromString("ns." + defaultZone);
        Name rname = Name.fromString("hostmaster." + defaultZone);
        return new SOARecord(soaZone, qclass, refresh, mname, rname, serial, refresh, retry, expire, refresh);
    }

}
