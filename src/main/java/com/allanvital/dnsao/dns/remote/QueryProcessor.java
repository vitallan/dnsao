package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.local.LocalResolver;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.dns.remote.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.notification.EventType;
import com.allanvital.dnsao.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.allanvital.dnsao.AppLoggers.DNS;
import static com.allanvital.dnsao.dns.remote.DnsUtils.*;
import static com.allanvital.dnsao.notification.QueryResolvedBy.*;
import static com.allanvital.dnsao.utils.ExceptionUtils.findRootCause;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessor {

    private static final Logger log = LoggerFactory.getLogger(DNS);
    private static final Set<Integer> ALLOWED_TYPES = Set.of(
            Type.A,
            Type.AAAA,
            Type.CNAME,
            Type.MX,
            Type.NS,
            Type.SOA,
            Type.PTR,
            Type.TXT,
            Type.SRV,
            Type.CAA,
            Type.HTTPS
    );

    private static final int MAX_RETRIES = 5;

    private final List<UpstreamResolver> resolvers = new LinkedList<>();
    private final CacheManager cacheManager;
    private final LocalResolver localResolver;
    private final DNSSecMode dnsSecMode;

    public QueryProcessor(List<UpstreamResolver> resolvers, CacheManager cacheManager, LocalResolver localResolver, DNSSecMode dnsSecMode) {
        this.localResolver = localResolver;
        this.dnsSecMode = dnsSecMode;
        this.resolvers.addAll(resolvers);
        this.cacheManager = cacheManager;
    }

    public DnsQuery processQuery(InetAddress clientAddress, byte[] data) {
        try {
            Message clientQuery = new Message(data);
            Message query = DnssecQueryShaper.prepareUpstreamQuery(clientQuery, dnsSecMode);
            Record question = query.getQuestion();
            Name name = question.getName();
            int type = question.getType();
            String typeName = Type.string(type);
            String client = clientAddress.getHostAddress();

            if (log.isTraceEnabled()) {
                logRequestHeaderAndFlags(query);
            }

            if (localResolver != null) {
                DnsQuery localResolved = localResolver.resolve(clientAddress, query);
                if (localResolved != null) {
                    return localResolved;
                }
            }

            DnsQuery dnsQuery = new DnsQuery(client);
            Message cached = getFromCache(name, type);
            if (cached != null) {
                Header header = cached.getHeader();
                header.setID(query.getHeader().getID());
                cached.setHeader(header);
                dnsQuery.setResolvedByAndResponse(CACHE, cached);
                return dnsQuery;
            }

            if (!ALLOWED_TYPES.contains(type)) {
                log.debug("Non-allowed query type {} to {} from {}:", typeName, name, client);
                Message refused = buildRefused(question, query.getHeader().getID());
                dnsQuery.setResolvedByAndResponse(REFUSED, refused);
                return dnsQuery;
            }

            log.trace("Query {} from {} to {} to be resolved", typeName, client, name);
            DnsQueryResult queryResult = query(query, resolvers, dnsSecMode, MAX_RETRIES);

            if (queryResult == null || queryResult.message() == null) {
                Message servFail = buildServFail(query);
                dnsQuery.setResolvedByAndResponse(SERVFAIL, servFail);
                return dnsQuery;
            }
            log.debug("Query {} from {} to {} solved by {}", typeName, client, name, queryResult.resolver().name());

            Message externalResponse = queryResult.message();
            int rcode = externalResponse.getRcode();
            externalResponse.getHeader().setID(query.getHeader().getID());
            putInCache(name, type, rcode, externalResponse);

            dnsQuery.setResolvedBySourceAndResponse(UPSTREAM, queryResult.resolver().getIp(), externalResponse);
            NotificationManager.getInstance().notify(EventType.QUERY_RESOLVED);
            return dnsQuery;

        } catch (WireParseException e) {
            DnsQuery query = new DnsQuery(null);
            if (hasValidQuestionSection(data)) {
                int id = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                Message response = new Message(id);
                Header header = response.getHeader();
                header.setFlag(Flags.QR);
                header.setRcode(Rcode.FORMERR);
                log.warn("DNS query from {} (len={}): {} was marked as FORMERR", clientAddress, data.length, e.getMessage());
                query.setResolvedByAndResponse(LOCAL, response);
            } else {
                log.warn("Invalid DNS query from {} (len={}): {}", clientAddress, data.length, e.getMessage());
            }
            return query;
        } catch (IOException | InterruptedException | TimeoutException e) {
            Throwable rootCause = findRootCause(e);
            log.error("Failed to process query: {} - {}", rootCause, rootCause.getMessage());
            return null;
        }
    }

    private void logRequestHeaderAndFlags(Message query) {
        boolean ad = query.getHeader().getFlag(Flags.AD);
        boolean cd = query.getHeader().getFlag(Flags.CD);
        List<Record> section = query.getSection(Section.ADDITIONAL);
        OPTRecord opt = null;
        for (Record r : section) {
            if (r instanceof OPTRecord) {
                opt = (OPTRecord) r;
                break;
            }
        }
        boolean doFlag = (opt != null) && ((opt.getFlags() & ExtendedFlags.DO) != 0);
        log.trace("REQ flags -> AD={}, CD={}, DO={}", ad, cd, doFlag);
    }

    private Message getFromCache(Name questionName, int type) {
        if (cacheManager == null) {
            return null;
        }
        return cacheManager.get(key(questionName, type));
    }

    private void putInCache(Name questionName, int type, int rcode, Message response) {
        if (cacheManager == null) {
            return;
        }
        Long ttlFromDirectResponse = getTtlFromDirectResponse(response);
        if (ttlFromDirectResponse != null) {
            cacheManager.put(key(questionName, type), response, ttlFromDirectResponse);
            return;
        }
        if (rcode == Rcode.NXDOMAIN) {
            SOARecord soa = findSOA(response);
            long negTtl = (soa != null) ? negativeTtlFrom(soa) : 300;
            cacheManager.put(key(questionName, type), response, negTtl);
        } else if (rcode == Rcode.NOERROR && response.getSection(Section.ANSWER).isEmpty()) {
            SOARecord soa = findSOA(response);
            if (soa != null) {
                long negTtl = negativeTtlFrom(soa);
                cacheManager.put(key(questionName, type), response, negTtl);
            }
        }
    }

    private String key(Name questionName, int type) {
        String typeName = Type.string(type);
        return typeName + ":" + questionName;
    }

}