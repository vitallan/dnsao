package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.remote.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import com.allanvital.dnsao.exc.DnsSecPolicyException;
import com.allanvital.dnsao.utils.ThreadShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsUtils {

    private static final Logger log = LoggerFactory.getLogger(DnsUtils.class);

    private static final long DEFAULT_LOCAL_TTL = 60L;

    public static SOARecord findSOA(Message msg) {
        for (Record r : msg.getSection(Section.AUTHORITY)) {
            if (r.getType() == Type.SOA) {
                return (SOARecord) r;
            }
        }
        return null;
    }

    public static long negativeTtlFrom(SOARecord soa) {
        long ttl = Math.min(soa.getMinimum(), soa.getTTL());
        ttl = Math.max(ttl, 60);
        ttl = Math.min(ttl, 300);
        return ttl;
    }

    public static boolean hasValidQuestionSection(byte[] data) {
        if (data.length < 12) return false;
        int qdCount = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        if (qdCount != 1) {
            return false;
        }
        int pos = 12;
        while (true) {
            if (pos >= data.length) {
                return false;
            }
            int labelLen = data[pos] & 0xFF;
            if (labelLen == 0) {
                pos++;
                break;
            }
            if (labelLen > 63) {
                return false;
            }
            pos += labelLen + 1;
        }
        int remaining = data.length - pos;
        return remaining >= 4;
    }

    public static Message buildRefused(org.xbill.DNS.Record question, int queryId) {
        Message refused = new Message(queryId);
        refused.getHeader().setFlag(Flags.QR);
        refused.getHeader().setRcode(Rcode.REFUSED);
        refused.addRecord(question, Section.QUESTION);
        return refused;
    }

    public static Message buildServFail(Message query) {
        Message fail = new Message(query.getHeader().getID());
        fail.getHeader().setFlag(Flags.QR);
        fail.addRecord(query.getQuestion(), Section.QUESTION);
        fail.getHeader().setRcode(Rcode.SERVFAIL);
        return fail;
    }

    public static boolean isBlocked(Name qname, Set<String> blockedSet) {
        String fqdn = qname.toString(true).toLowerCase(Locale.ROOT);
        if (fqdn.endsWith(".")) {
            fqdn = fqdn.substring(0, fqdn.length() - 1);
        }

        if (blockedSet.contains(fqdn)) return true;
        int dot = fqdn.indexOf('.');
        while (dot != -1 && dot + 1 < fqdn.length()) {
            String suffix = fqdn.substring(dot + 1);
            if (blockedSet.contains(suffix)) return true;
            dot = fqdn.indexOf('.', dot + 1);
        }
        return false;
    }

    private static Message formErr(Message req) {
        Message m = new Message(req.getHeader().getID());
        m.getHeader().setFlag(Flags.QR);
        m.getHeader().setRcode(Rcode.FORMERR);
        return m;
    }

    private static Message baseResponse(Message request, Record question) {
        Message resp = new Message(request.getHeader().getID());

        Header h = resp.getHeader();
        h.setFlag(Flags.QR);
        if (request.getHeader().getFlag(Flags.RD)) h.setFlag(Flags.RD);
        h.setFlag(Flags.RA);

        resp.addRecord(question, Section.QUESTION);

        OPTRecord opt = request.getOPT();
        if (opt != null) resp.addRecord(opt, Section.ADDITIONAL);

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

    public static Message buildLocalResponse(Message request, String targetIpv4Ip) {
        Record q = request.getQuestion();
        if (q == null) return formErr(request);

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

    public static Message buildBlocked(Message query) throws UnknownHostException, TextParseException {
        Record question = query.getQuestion();
        if (question == null) return formErr(query);

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
        long serial = System.currentTimeMillis() / 1000L;

        Name soaZone = Name.fromString(defaultZone);
        Name mname = Name.fromString("ns." + defaultZone);
        Name rname = Name.fromString("hostmaster." + defaultZone);
        return new SOARecord(soaZone, qclass, refresh, mname, rname, serial, refresh, retry, expire, refresh);
    }

    public static DnsQueryResult query(Message query, List<NamedResolver> resolvers, DNSSecMode dnsSecMode)
            throws InterruptedException, TimeoutException {

        String threadName = Thread.currentThread().getName();
        ExecutorService executor = ThreadShop.buildExecutor(threadName + "-res", resolvers.size());
        try {
            List<Callable<DnsQueryResult>> tasks = resolvers.stream()
                    .<Callable<DnsQueryResult>>map(resolver -> () -> {
                        Message response = resolver.send(query);
                        if (response == null) {
                            throw new IOException("Null response");
                        }
                        if (shouldRejectByDnssecPolicy(response, dnsSecMode)) {
                            throw new DnsSecPolicyException("non accepted answer based on dnssec policy");
                        }
                        return new DnsQueryResult(response, resolver);
                    })
                    .toList();

            return executor.invokeAny(tasks, 3, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            String question = query.getQuestion().toString();
            log.warn("query {} failed. Cause: {}", question, e.getMessage());
            return null;
        }
        finally {
            executor.shutdownNow();
        }
    }

    public static boolean shouldRejectByDnssecPolicy(Message response, DNSSecMode dnsSecMode) {
        final int rcode = response.getRcode();
        final boolean ad = response.getHeader().getFlag(Flags.AD);
        return switch (dnsSecMode) {
            case SIMPLE -> rcode == Rcode.SERVFAIL;
            case RIGID -> !ad;
            default -> false;
        };
    }

    public static boolean isDirectAnswer(int type) {
        return type == Type.A || type == Type.AAAA || type == Type.CNAME;
    }

    public static boolean isWarmable(Message msg) {
        return getTtlFromDirectResponse(msg) != null;
    }

    public static Long getTtlFromDirectResponse(Message message) {
        if (message == null || message.getRcode() != Rcode.NOERROR) {
            return null;
        }
        List<Record> section = message.getSection(Section.ANSWER);
        if (section == null || section.isEmpty()) {
            return null;
        }
        for (Record r : section) {
            if (isDirectAnswer(r.getType())) {
                return r.getTTL();
            }
        }
        return null;
    }

}