package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSession {

    private static final int MAX_ITERATIONS = 30;
    private static final AtomicLong SESSION_COUNTER = new AtomicLong(1);

    private final StepRequest request;
    private final ServerRacer serverRacer;
    private final RootHintsProvider rootHintsProvider;
    private final RecursiveCache recursiveCache;
    private final DNSSecMode dnsSecMode;
    private final RecursiveStatsCollector recursiveStatsCollector;
    private final long sessionId;
    private int stepCount;

    public RecursiveSession(DnsQueryRequest dnsQueryRequest, ServerRacer serverRacer, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, DNSSecMode dnsSecMode) {
        this(dnsQueryRequest, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, new NoOpRecursiveStatsCollector());
    }

    public RecursiveSession(DnsQueryRequest dnsQueryRequest, ServerRacer serverRacer, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, DNSSecMode dnsSecMode, RecursiveStatsCollector recursiveStatsCollector) {
        this.request = StepRequest.fromMessage(dnsQueryRequest, dnsSecMode);
        this.serverRacer = serverRacer;
        this.rootHintsProvider = rootHintsProvider;
        this.recursiveCache = recursiveCache;
        this.dnsSecMode = dnsSecMode;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.sessionId = SESSION_COUNTER.getAndIncrement();
    }

    public Message resolve() {
        if (request == null) {
            return null;
        }
        recursiveStatsCollector.increment(RecursiveMetric.SESSION_STARTED);
        long startNs = System.nanoTime();
        Log.DNS.trace("recursive start session={} qtype={} qname={} dnssec={}", sessionId, Type.string(request.qtype()), request.qname(), dnsSecMode);
        StepResponse stepResponse = resolveInternal(request.qname(), request.qtype(), new HashSet<>());
        recursiveStatsCollector.add(RecursiveMetric.SESSION_STEP_SUM, stepCount);
        recursiveStatsCollector.add(RecursiveMetric.SESSION_ELAPSED_MS_SUM, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs));
        if (stepResponse == null) {
            recursiveStatsCollector.increment(RecursiveMetric.SESSION_FAILED);
            Log.DNS.trace("recursive done session={} qtype={} qname={} outcome=failed steps={}", sessionId, Type.string(request.qtype()), request.qname(), stepCount);
            return null;
        }
        if (stepResponse.isNXDOMAIN()) {
            recursiveStatsCollector.increment(RecursiveMetric.SESSION_NXDOMAIN);
        } else if (stepResponse.isNoDataFor(request.qname(), request.qtype())) {
            recursiveStatsCollector.increment(RecursiveMetric.SESSION_NODATA);
        } else {
            recursiveStatsCollector.increment(RecursiveMetric.SESSION_SUCCEEDED);
        }
        Log.DNS.trace("recursive done session={} qtype={} qname={} rcode={} steps={}", sessionId, Type.string(request.qtype()), request.qname(), stepResponse.toWireMessage().getRcode(), stepCount);
        return stepResponse.toWireMessage();
    }

    private StepResponse resolveInternal(Name qname, int qtype) {
        return resolveInternal(qname, qtype, new HashSet<>());
    }

    private StepResponse resolveInternal(Name qname, int qtype, Set<Name> cnamePath) {
        if (!cnamePath.add(qname)) {
            return null;
        }
        if (cnamePath.size() > MAX_ITERATIONS) {
            cnamePath.remove(qname);
            return null;
        }

        List<NameServerAddress> authoritativeServers = resolveAuthoritativeServers(qname);
        if (authoritativeServers.isEmpty()) {
            cnamePath.remove(qname);
            return null;
        }
        StepResponse response = resolveFinalQuery(qname, qtype, authoritativeServers, cnamePath);
        cnamePath.remove(qname);
        return response;
    }

    private List<NameServerAddress> resolveAuthoritativeServers(Name qname) {
        List<NameServerAddress> currentServers = rootHintsProvider.getRootServers();
        List<Name> minimizedNames = buildMinimizedNsNames(qname);

        for (int i = 0; i < minimizedNames.size(); i++) {
            Name minimizedName = minimizedNames.get(i);
            StepRequest stepRequest = new StepRequest(minimizedName, Type.NS, request.qclass(), dnsSecMode);
            recursiveStatsCollector.increment(RecursiveMetric.WALK_STEP);
            stepCount++;
            Log.DNS.trace("recursive step session={} phase=walk qtype={} qname={} servers={}", sessionId, Type.string(stepRequest.qtype()), stepRequest.qname(), currentServers.size());
            boolean lastStep = i == minimizedNames.size() - 1;
            if (lastStep) {
                currentServers = resolveNextServers(currentServers, stepRequest);
            } else {
                currentServers = resolveNavigationServers(currentServers, stepRequest);
            }
            if (currentServers.isEmpty()) {
                return List.of();
            }
        }

        return currentServers;
    }

    private StepResponse resolveFinalQuery(Name qname, int qtype, List<NameServerAddress> servers, Set<Name> cnamePath) {
        Name currentName = qname;
        List<NameServerAddress> currentServers = servers;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            StepRequest stepRequest = new StepRequest(currentName, qtype, request.qclass(), dnsSecMode);
            recursiveStatsCollector.increment(RecursiveMetric.FINAL_STEP);
            stepCount++;
            Log.DNS.trace("recursive step session={} phase=final qtype={} qname={} servers={} iteration={}", sessionId, Type.string(stepRequest.qtype()), stepRequest.qname(), currentServers.size(), iteration + 1);
            StepResponse response = queryServers(currentServers, stepRequest);
            if (response == null) {
                return null;
            }

            if (response.isNXDOMAIN()) {
                return response;
            }

            if (response.hasAnswer(currentName, qtype)) {
                return response;
            }

            if (response.isNoDataFor(currentName, qtype)) {
                return response;
            }

            Name cnameTarget = response.getCnameTarget(currentName);
            if (cnameTarget != null) {
                recursiveStatsCollector.increment(RecursiveMetric.CNAME_FOLLOWED);
                Log.DNS.trace("recursive cname session={} from={} to={}", sessionId, currentName, cnameTarget);
                StepResponse targetResponse = resolveInternal(cnameTarget, qtype, cnamePath);
                if (targetResponse == null) {
                    return null;
                }
                if (targetResponse.toWireMessage().getRcode() != Rcode.NOERROR) {
                    return targetResponse;
                }
                return buildClientFacingResponse(qname, qtype, response.getCnameAnswers(currentName), targetResponse.getAnswerRecords());
            }

            List<NameServerAddress> referralServers = response.getReferralServers();
            if (!referralServers.isEmpty()) {
                recursiveStatsCollector.increment(RecursiveMetric.REFERRAL_FOLLOWED);
                Log.DNS.trace("recursive referral session={} qname={} nextServers={}", sessionId, currentName, referralServers.size());
                currentServers = referralServers;
                continue;
            }

            List<Name> nsTargets = response.getNSTargets();
            if (!nsTargets.isEmpty()) {
                List<NameServerAddress> resolved = resolveNsTargets(nsTargets);
                if (!resolved.isEmpty()) {
                    currentServers = resolved;
                    continue;
                }
            }

            return null;
        }

        return null;
    }

    private List<NameServerAddress> resolveNextServers(List<NameServerAddress> currentServers, StepRequest request) {
        StepResponse response = queryServers(currentServers, request);
        if (response == null || response.isNXDOMAIN()) {
            return List.of();
        }

        List<NameServerAddress> referralServers = response.getReferralServers();
        if (!referralServers.isEmpty()) {
            return referralServers;
        }

        List<Name> nsTargets = response.getNSTargets();
        if (!nsTargets.isEmpty()) {
            List<NameServerAddress> resolvedTargets = resolveNsTargets(nsTargets);
            if (!resolvedTargets.isEmpty()) {
                return resolvedTargets;
            }
        }

        return currentServers;
    }

    private List<NameServerAddress> resolveNavigationServers(List<NameServerAddress> currentServers, StepRequest request) {
        StepResponse cachedResponse = recursiveCache.get(request);
        if (cachedResponse != null) {
            if (cachedResponse.isNXDOMAIN()) {
                return List.of();
            }
            return currentServers;
        }

        Map.Entry<NameServerAddress, StepResponse> responseEntry = serverRacer.race(currentServers, request);
        if (responseEntry == null || responseEntry.getValue().isNXDOMAIN()) {
            return List.of();
        }
        recursiveCache.put(request, responseEntry.getValue());
        NameServerAddress respondingServer = responseEntry.getKey();
        return List.of(new NameServerAddress(respondingServer.ip(), respondingServer.port()));
    }

    private StepResponse queryServers(List<NameServerAddress> servers, StepRequest request) {
        StepResponse cachedResponse = recursiveCache.get(request);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Map.Entry<NameServerAddress, StepResponse> responseEntry = serverRacer.race(servers, request);
        if (responseEntry == null) {
            return null;
        }
        StepResponse response = responseEntry.getValue();
        recursiveCache.put(request, response);
        return response;
    }

    private List<NameServerAddress> resolveNsTargets(List<Name> targets) {
        for (Name target : targets) {
            List<NameServerAddress> addresses = resolveNsTargetAddresses(target);
            if (!addresses.isEmpty()) {
                return addresses;
            }
        }
        return List.of();
    }

    private List<NameServerAddress> resolveNsTargetAddresses(Name target) {
        recursiveStatsCollector.increment(RecursiveMetric.HELPER_RESOLVE_A);
        Log.DNS.trace("recursive helper session={} qtype=A qname={}", sessionId, target);
        StepResponse ipv4Response = resolveInternal(target, Type.A);
        if (ipv4Response != null) {
            List<NameServerAddress> ipv4Addresses = ipv4Response.getARecordAddresses(target);
            if (!ipv4Addresses.isEmpty()) {
                return ipv4Addresses;
            }
        }

        recursiveStatsCollector.increment(RecursiveMetric.HELPER_RESOLVE_AAAA);
        Log.DNS.trace("recursive helper session={} qtype=AAAA qname={}", sessionId, target);
        StepResponse ipv6Response = resolveInternal(target, Type.AAAA);
        if (ipv6Response == null) {
            return List.of();
        }
        return ipv6Response.getAaaaRecordAddresses(target);
    }

    private List<Name> buildMinimizedNsNames(Name qname) {
        String normalized = qname.toString();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            return List.of();
        }

        String[] labels = normalized.split("\\.");
        List<Name> result = new ArrayList<>();
        result.add(toName(labels[labels.length - 1]));

        if (labels.length > 1) {
            String joined = String.join(".", Arrays.copyOfRange(labels, 0, labels.length));
            Name fullName = toName(joined);
            if (!fullName.equals(result.get(0))) {
                result.add(fullName);
            }
        }
        return result;
    }

    private StepResponse buildClientFacingResponse(Name qname, int qtype, List<Record> leadingAnswers, List<Record> trailingAnswers) {
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(Record.newRecord(qname, qtype, request.qclass()), Section.QUESTION);
        addAnswerRecords(response, leadingAnswers);
        addAnswerRecords(response, trailingAnswers);
        return new StepResponse(response);
    }

    private void addAnswerRecords(Message response, List<Record> answers) {
        for (Record answer : answers) {
            response.addRecord(answer, Section.ANSWER);
        }
    }

    private Name toName(String qname) {
        try {
            return Name.fromString(qname.endsWith(".") ? qname : qname + ".");
        } catch (org.xbill.DNS.TextParseException e) {
            throw new IllegalArgumentException("invalid qname: " + qname, e);
        }
    }

}
