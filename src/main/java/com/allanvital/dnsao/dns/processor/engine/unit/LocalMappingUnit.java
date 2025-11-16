package com.allanvital.dnsao.dns.processor.engine.unit;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.allanvital.dnsao.dns.remote.DnsUtils.baseResponse;
import static com.allanvital.dnsao.dns.remote.DnsUtils.formErr;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.LOCAL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LocalMappingUnit implements EngineUnit {

    private final Map<String, String> localMappings;

    public LocalMappingUnit(Map<String, String> localMappings) {
        this.localMappings = localMappings;
    }

    @Override
    public DnsQueryResponse innerProcess(DnsQueryRequest dnsQueryRequest) {
        Message request = dnsQueryRequest.getRequest();
        String name = getQuestionName(request).toString().toLowerCase();
        if (!localMappings.containsKey(name)) {
            return null;
        }
        String responseIp = localMappings.get(name);
        Message response = buildLocalResponse(request, responseIp);
        return new DnsQueryResponse(dnsQueryRequest, response);
    }

    @Override
    public QueryResolvedBy unitResolvedBy() {
        return LOCAL;
    }

    public static Message buildLocalResponse(Message request, String targetIpv4Ip) {
        Record q = request.getQuestion();
        if (q == null) {
            return formErr(request);
        }

        Name qname = q.getName();
        int qtype = q.getType();
        int qclass = q.getDClass();

        Message resp = baseResponse(request, q);

        if (qclass == DClass.IN && (qtype == Type.A || qtype == Type.ANY)) {
            Inet4Address v4 = asInet4(targetIpv4Ip);
            ARecord arec = new ARecord(qname, DClass.IN, DEFAULT_LOCAL_TTL, v4);
            resp.addRecord(arec, Section.ANSWER);
        }

        return resp;
    }

    private static Inet4Address asInet4(String ip) {
        try {
            InetAddress a = InetAddress.getByName(ip);
            if (!(a instanceof Inet4Address)) {
                throw new IllegalArgumentException("ip is not ipv4: " + ip);
            }
            return (Inet4Address) a;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("invalid ipv4: " + ip, e);
        }
    }

}
