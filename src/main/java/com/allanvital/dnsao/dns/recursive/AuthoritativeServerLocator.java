package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Name;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class AuthoritativeServerLocator {

    private final RootHintsProvider rootHintsProvider;
    private final RecursiveQueryExecutor queryExecutor;
    private final NameServerTargetResolver nameServerTargetResolver;
    private final RecursiveStatsCollector recursiveStatsCollector;
    private final RecursiveSessionState sessionState;
    private final DNSSecMode dnsSecMode;
    private final long sessionId;

    AuthoritativeServerLocator(RootHintsProvider rootHintsProvider,
                               RecursiveQueryExecutor queryExecutor,
                               NameServerTargetResolver nameServerTargetResolver,
                               RecursiveStatsCollector recursiveStatsCollector,
                               RecursiveSessionState sessionState,
                               DNSSecMode dnsSecMode,
                               long sessionId) {
        this.rootHintsProvider = rootHintsProvider;
        this.queryExecutor = queryExecutor;
        this.nameServerTargetResolver = nameServerTargetResolver;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.sessionState = sessionState;
        this.dnsSecMode = dnsSecMode;
        this.sessionId = sessionId;
    }

    List<NameServerAddress> resolveAuthoritativeServers(Name qname, int qclass) {
        List<NameServerAddress> currentServers = rootHintsProvider.getRootServers();
        List<Name> minimizedNames = buildMinimizedNsNames(qname);

        for (int i = 0; i < minimizedNames.size(); i++) {
            if (!sessionState.hasRemainingSessionBudget()) {
                return List.of();
            }
            Name minimizedName = minimizedNames.get(i);
            StepRequest stepRequest = new StepRequest(minimizedName, Type.NS, qclass, dnsSecMode);
            recursiveStatsCollector.increment(RecursiveMetric.WALK_STEP);
            sessionState.recordStep();
            Log.DNS.trace("recursive step session={} phase=walk qtype={} qname={} servers={}", sessionId, Type.string(stepRequest.qtype()), stepRequest.qname(), currentServers.size());
            boolean lastStep = i == minimizedNames.size() - 1;
            if (lastStep) {
                currentServers = resolveNextServers(currentServers, stepRequest);
            } else {
                currentServers = resolveNavigationServers(currentServers, stepRequest);
            }
            if (!sessionState.hasRemainingSessionBudget()) {
                return List.of();
            }
            if (currentServers.isEmpty()) {
                return List.of();
            }
        }

        return currentServers;
    }

    private List<NameServerAddress> resolveNextServers(List<NameServerAddress> currentServers, StepRequest request) {
        StepResponse response = queryExecutor.queryServers(currentServers, request);
        if (response == null || response.isNXDOMAIN()) {
            return List.of();
        }

        List<NameServerAddress> referralServers = response.getReferralServers();
        if (!referralServers.isEmpty()) {
            return referralServers;
        }

        List<Name> nsTargets = response.getNSTargets();
        if (!nsTargets.isEmpty()) {
            List<NameServerAddress> resolved = nameServerTargetResolver.resolveNsTargets(nsTargets);
            if (!resolved.isEmpty()) {
                return resolved;
            }
            if (sessionState.isHelperBudgetExhausted()) {
                return List.of();
            }
        }

        return currentServers;
    }

    private List<NameServerAddress> resolveNavigationServers(List<NameServerAddress> currentServers, StepRequest request) {
        StepResponse response = queryExecutor.queryServers(currentServers, request);
        if (response == null || response.isNXDOMAIN()) {
            return List.of();
        }

        List<Name> nsTargets = response.getNSTargets();
        if (shouldKeepCurrentServers(nsTargets, request.qname())) {
            return currentServers;
        }

        List<NameServerAddress> referralServers = response.getReferralServers();
        if (!referralServers.isEmpty()) {
            return referralServers;
        }

        if (!nsTargets.isEmpty()) {
            List<NameServerAddress> resolved = nameServerTargetResolver.resolveNsTargets(nsTargets);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }

        return List.of();
    }

    private boolean shouldKeepCurrentServers(List<Name> nsTargets, Name qname) {
        if (nsTargets.isEmpty()) {
            return false;
        }
        for (Name target : nsTargets) {
            if (!target.subdomain(qname)) {
                return false;
            }
        }
        return true;
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
        for (int i = labels.length - 1; i > 0; i--) {
            result.add(toName(String.join(".", Arrays.copyOfRange(labels, i, labels.length))));
        }
        if (labels.length == 2) {
            result.add(toName(normalized));
        }
        return result;
    }

    private Name toName(String qname) {
        try {
            return Name.fromString(qname.endsWith(".") ? qname : qname + ".");
        } catch (org.xbill.DNS.TextParseException e) {
            throw new IllegalArgumentException("invalid qname: " + qname, e);
        }
    }
}
