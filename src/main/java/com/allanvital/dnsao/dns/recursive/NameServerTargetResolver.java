package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Name;
import org.xbill.DNS.Type;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class NameServerTargetResolver {

    private final RecursiveLookup recursiveLookup;
    private final RecursiveStatsCollector recursiveStatsCollector;
    private final RecursiveSessionState sessionState;
    private final long sessionId;

    NameServerTargetResolver(RecursiveLookup recursiveLookup,
                             RecursiveStatsCollector recursiveStatsCollector,
                             RecursiveSessionState sessionState,
                             long sessionId) {
        this.recursiveLookup = recursiveLookup;
        this.recursiveStatsCollector = recursiveStatsCollector;
        this.sessionState = sessionState;
        this.sessionId = sessionId;
    }

    List<NameServerAddress> resolveNsTargets(List<Name> targets) {
        for (Name target : targets) {
            if (!sessionState.tryAcquireHelperResolutionBudget(target)) {
                return List.of();
            }
            List<NameServerAddress> addresses = resolveNsTargetAddresses(target);
            if (!addresses.isEmpty()) {
                return addresses;
            }
        }
        return List.of();
    }

    private List<NameServerAddress> resolveNsTargetAddresses(Name target) {
        if (!sessionState.hasRemainingSessionBudget()) {
            return List.of();
        }
        recursiveStatsCollector.increment(RecursiveMetric.HELPER_RESOLVE_A);
        Log.DNS.trace("recursive helper session={} qtype=A qname={}", sessionId, target);
        StepResponse ipv4Response = recursiveLookup.resolve(target, Type.A);
        if (ipv4Response != null) {
            List<NameServerAddress> ipv4Addresses = ipv4Response.getARecordAddresses(target);
            if (!ipv4Addresses.isEmpty()) {
                return ipv4Addresses;
            }
        }

        if (!sessionState.hasRemainingSessionBudget()) {
            return List.of();
        }
        recursiveStatsCollector.increment(RecursiveMetric.HELPER_RESOLVE_AAAA);
        Log.DNS.trace("recursive helper session={} qtype=AAAA qname={}", sessionId, target);
        StepResponse ipv6Response = recursiveLookup.resolve(target, Type.AAAA);
        if (ipv6Response == null) {
            return List.of();
        }
        return ipv6Response.getAaaaRecordAddresses(target);
    }
}
