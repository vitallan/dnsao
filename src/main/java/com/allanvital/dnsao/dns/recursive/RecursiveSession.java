package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.RRSIGRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.allanvital.dnsao.dns.recursive.RecursiveMetric.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSession {

    private static final int MAX_ITERATIONS = 30;
    private static final int DEFAULT_MAX_NS_NAME_RESOLUTIONS = 12;
    private static final long DEFAULT_MAX_SESSION_ELAPSED_MS = 10_000L;
    private static final AtomicLong SESSION_COUNTER = new AtomicLong(1);

    private final StepRequest request;
    private final DNSSecMode dnsSecMode;
    private final RecursiveStatsCollector recursiveStatsCollector;
    private final long sessionId;
    private final RecursiveSessionState sessionState;
    private final RecursiveQueryExecutor queryExecutor;
    private final NameServerTargetResolver nameServerTargetResolver;
    private final AuthoritativeServerLocator authoritativeServerLocator;

    public RecursiveSession(DnsQueryRequest dnsQueryRequest, ServerRacer serverRacer, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, DNSSecMode dnsSecMode) {
        this(dnsQueryRequest, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, new NoOpRecursiveStatsCollector(), DEFAULT_MAX_NS_NAME_RESOLUTIONS, DEFAULT_MAX_SESSION_ELAPSED_MS);
    }

    public RecursiveSession(DnsQueryRequest dnsQueryRequest, ServerRacer serverRacer, RootHintsProvider rootHintsProvider, RecursiveCache recursiveCache, DNSSecMode dnsSecMode, RecursiveStatsCollector recursiveStatsCollector) {
        this(dnsQueryRequest, serverRacer, rootHintsProvider, recursiveCache, dnsSecMode, recursiveStatsCollector, DEFAULT_MAX_NS_NAME_RESOLUTIONS, DEFAULT_MAX_SESSION_ELAPSED_MS);
    }

    public RecursiveSession(DnsQueryRequest dnsQueryRequest,
                            ServerRacer serverRacer,
                            RootHintsProvider rootHintsProvider,
                            RecursiveCache recursiveCache,
                            DNSSecMode dnsSecMode,
                            RecursiveStatsCollector recursiveStatsCollector,
                            int maxNsNameResolutions,
                            long maxSessionElapsedMillis) {
        this.request = StepRequest.fromMessage(dnsQueryRequest, dnsSecMode);
        this.dnsSecMode = dnsSecMode;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.sessionId = SESSION_COUNTER.getAndIncrement();
        this.sessionState = new RecursiveSessionState(maxNsNameResolutions, maxSessionElapsedMillis);
        this.queryExecutor = new RecursiveQueryExecutor(recursiveCache, serverRacer, sessionState);
        this.nameServerTargetResolver = new NameServerTargetResolver(this::resolveInternal, recursiveStatsCollector, sessionState, sessionId);
        this.authoritativeServerLocator = new AuthoritativeServerLocator(rootHintsProvider, queryExecutor, nameServerTargetResolver, recursiveStatsCollector, sessionState, dnsSecMode, sessionId);
    }

    public Message resolve() {
        if (request == null) {
            return null;
        }
        sessionState.start(sessionId, request.qname());
        recursiveStatsCollector.increment(SESSION_STARTED);
        long startNs = System.nanoTime();
        Log.DNS.trace("recursive start session={} qtype={} qname={} dnssec={}", sessionId, Type.string(request.qtype()), request.qname(), dnsSecMode);
        StepResponse stepResponse = resolveInternal(request.qname(), request.qtype(), new HashSet<>());
        recursiveStatsCollector.add(SESSION_STEP_SUM, sessionState.stepCount());
        recursiveStatsCollector.add(SESSION_ELAPSED_MS_SUM, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs));
        if (stepResponse == null) {
            recursiveStatsCollector.increment(RecursiveMetric.SESSION_FAILED);
            Log.DNS.trace("recursive done session={} qtype={} qname={} outcome=failed steps={}", sessionId, Type.string(request.qtype()), request.qname(), sessionState.stepCount());
            return null;
        }
        if (stepResponse.isNXDOMAIN()) {
            recursiveStatsCollector.increment(SESSION_NXDOMAIN);
        } else if (stepResponse.isNoDataFor(request.qname(), request.qtype())) {
            recursiveStatsCollector.increment(SESSION_NODATA);
        } else {
            recursiveStatsCollector.increment(SESSION_SUCCEEDED);
        }
        Log.DNS.trace("recursive done session={} qtype={} qname={} rcode={} steps={}", sessionId, Type.string(request.qtype()), request.qname(), stepResponse.toWireMessage().getRcode(), sessionState.stepCount());
        return stepResponse.toWireMessage();
    }

    private StepResponse resolveInternal(Name qname, int qtype) {
        return resolveInternal(qname, qtype, new HashSet<>());
    }

    private StepResponse resolveInternal(Name qname, int qtype, Set<Name> cnamePath) {
        if (!sessionState.hasRemainingSessionBudget()) {
            return null;
        }
        if (!cnamePath.add(qname)) {
            return null;
        }
        if (cnamePath.size() > MAX_ITERATIONS) {
            cnamePath.remove(qname);
            return null;
        }

        List<NameServerAddress> authoritativeServers = authoritativeServerLocator.resolveAuthoritativeServers(qname, request.qclass());
        if (authoritativeServers.isEmpty()) {
            cnamePath.remove(qname);
            return null;
        }
        StepResponse response = resolveFinalQuery(qname, qtype, authoritativeServers, cnamePath);
        cnamePath.remove(qname);
        return response;
    }

    private StepResponse resolveFinalQuery(Name qname, int qtype, List<NameServerAddress> servers, Set<Name> cnamePath) {
        Name currentName = qname;
        List<NameServerAddress> currentServers = new ArrayList<>(servers);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            if (!sessionState.hasRemainingSessionBudget()) {
                return null;
            }
            StepRequest stepRequest = new StepRequest(currentName, qtype, request.qclass(), dnsSecMode);
            recursiveStatsCollector.increment(RecursiveMetric.FINAL_STEP);
            sessionState.recordStep();
            Log.DNS.trace("recursive step session={} phase=final qtype={} qname={} servers={} iteration={}", sessionId, Type.string(stepRequest.qtype()), stepRequest.qname(), currentServers.size(), iteration + 1);
            NameServerAddress respondingServer;
            StepResponse response;
            boolean usedFreshQuery;
            if (currentServers.size() == 1) {
                usedFreshQuery = false;
                respondingServer = currentServers.get(0);
                response = queryExecutor.queryServers(currentServers, stepRequest);
                if (response == null) {
                    return null;
                }
            } else {
                usedFreshQuery = true;
                java.util.Map.Entry<NameServerAddress, StepResponse> responseEntry = queryExecutor.queryServersFresh(currentServers, stepRequest);
                if (responseEntry == null) {
                    return null;
                }
                respondingServer = responseEntry.getKey();
                response = responseEntry.getValue();
            }
            if (!sessionState.hasRemainingSessionBudget()) {
                return null;
            }

            if (response.isNXDOMAIN()) {
                if (usedFreshQuery) {
                    queryExecutor.cacheResponse(stepRequest, response);
                }
                return response;
            }

            if (response.hasAnswer(currentName, qtype)) {
                if (usedFreshQuery) {
                    queryExecutor.cacheResponse(stepRequest, response);
                }
                return buildClientFacingPositiveResponse(qname, qtype, response.getAnswerRecords());
            }

            if (response.isNoDataFor(currentName, qtype)) {
                if (usedFreshQuery) {
                    queryExecutor.cacheResponse(stepRequest, response);
                }
                return response;
            }

            Name cnameTarget = response.getCnameTarget(currentName);
            if (cnameTarget != null) {
                recursiveStatsCollector.increment(RecursiveMetric.CNAME_FOLLOWED);
                Log.DNS.trace("recursive cname session={} from={} to={}", sessionId, currentName, cnameTarget);
                if (!sessionState.hasRemainingSessionBudget()) {
                    return null;
                }
                StepResponse targetResponse = resolveInternal(cnameTarget, qtype, cnamePath);
                if (targetResponse == null) {
                    currentServers = excludeServer(currentServers, respondingServer);
                    if (!currentServers.isEmpty()) {
                        continue;
                    }
                    return null;
                }
                if (targetResponse.toWireMessage().getRcode() != Rcode.NOERROR) {
                    return targetResponse;
                }
                return buildClientFacingResponse(qname, qtype, response.getCnameAnswers(currentName), targetResponse.getAnswerRecords());
            }

            List<NameServerAddress> referralServers = response.getReferralServers();
            if (!referralServers.isEmpty()) {
                Set<String> currentIps = currentServers.stream().map(NameServerAddress::ip).collect(Collectors.toSet());
                Set<String> referralIps = referralServers.stream().map(NameServerAddress::ip).collect(Collectors.toSet());
                if (currentIps.containsAll(referralIps)) {
                    Log.DNS.trace("recursive referral loop session={} qname={} servers={}", sessionId, currentName, referralServers.size());
                    sessionState.markReferralLoopDetected();
                    return null;
                } else {
                    recursiveStatsCollector.increment(RecursiveMetric.REFERRAL_FOLLOWED);
                    Log.DNS.trace("recursive referral session={} qname={} nextServers={}", sessionId, currentName, referralServers.size());
                    currentServers = referralServers;
                    continue;
                }
            }

            List<Name> nsTargets = response.getNSTargets();
            if (!nsTargets.isEmpty()) {
                List<NameServerAddress> resolved = nameServerTargetResolver.resolveNsTargets(nsTargets);
                if (!resolved.isEmpty()) {
                    currentServers = resolved;
                    continue;
                }
            }

            currentServers = excludeServer(currentServers, respondingServer);
            if (!currentServers.isEmpty()) {
                continue;
            }
            return null;
        }

        return null;
    }

    private List<NameServerAddress> excludeServer(List<NameServerAddress> servers, NameServerAddress respondingServer) {
        if (respondingServer == null || servers.size() <= 1) {
            return List.of();
        }
        List<NameServerAddress> remaining = new ArrayList<>();
        for (NameServerAddress server : servers) {
            if (server.ip().equals(respondingServer.ip()) && server.port() == respondingServer.port()) {
                continue;
            }
            remaining.add(server);
        }
        return List.copyOf(remaining);
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

    private StepResponse buildClientFacingPositiveResponse(Name qname, int qtype, List<Record> authoritativeAnswers) {
        Message response = new Message();
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(Record.newRecord(qname, qtype, request.qclass()), Section.QUESTION);
        for (Record answer : authoritativeAnswers) {
            if (answer.getName().equals(qname) && answer.getType() == qtype) {
                response.addRecord(answer, Section.ANSWER);
                continue;
            }
            if (answer instanceof RRSIGRecord rrsig
                    && answer.getName().equals(qname)
                    && rrsig.getTypeCovered() == qtype) {
                response.addRecord(answer, Section.ANSWER);
            }
        }
        return new StepResponse(response);
    }

}
