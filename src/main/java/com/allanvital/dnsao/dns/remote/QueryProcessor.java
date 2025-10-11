package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.cache.CacheManager;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import com.allanvital.dnsao.dns.remote.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

    private final List<NamedResolver> resolvers = new LinkedList<>();
    private final CacheManager cacheManager;
    private final Set<String> blockedList;
    private final Map<String, String> localMappings;

    public QueryProcessor(List<NamedResolver> resolvers, CacheManager cacheManager, Set<String> blockedList, Map<String, String> localMappings) {
        if (blockedList != null) {
            this.blockedList = blockedList;
        } else {
            this.blockedList = new TreeSet<>();
        }
        if (localMappings != null) {
            this.localMappings = localMappings;
        } else {
            this.localMappings = new HashMap<>();
        }
        this.resolvers.addAll(resolvers);
        this.cacheManager = cacheManager;
    }

    public DnsQuery processQuery(InetAddress clientAddress, byte[] data) {
        try {
            Message query = new Message(data);
            Record question = query.getQuestion();
            Name name = question.getName();
            int type = question.getType();
            String typeName = Type.string(type);
            String client = clientAddress.getHostAddress();
            DnsQuery dnsQuery = new DnsQuery(client);

            if (isBlocked(name, blockedList)) {
                Message blocked = buildBlocked(query);
                dnsQuery.setResolvedByAndResponse(BLOCKED, blocked);
                return dnsQuery;
            }

            if (localMappings.containsKey(name.toString().toLowerCase())) {
                String ip = localMappings.get(name.toString().toLowerCase());
                Message localAnswer = buildLocalResponse(query, ip);
                dnsQuery.setResolvedByAndResponse(LOCAL, localAnswer);
                return dnsQuery;
            }

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

            log.debug("Query {} from {} to {}", typeName, client, name);
            DnsQueryResult queryResult = query(query, resolvers);
            Message externalResponse = queryResult.message();
            if (externalResponse == null) {
                Message servFail = buildServFail(query);
                dnsQuery.setResolvedByAndResponse(SERVFAIL, servFail);
                return dnsQuery;
            }
            int rcode = externalResponse.getRcode();
            externalResponse.getHeader().setID(query.getHeader().getID());
            putInCache(name, type, rcode, externalResponse);

            dnsQuery.setResolvedBySourceAndResponse(UPSTREAM, queryResult.resolver().getIp(), externalResponse);

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
        } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
            Throwable rootCause = findRootCause(e);
            log.error("Failed to process query: {} - {}", rootCause, rootCause.getMessage());
            return null;
        }
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