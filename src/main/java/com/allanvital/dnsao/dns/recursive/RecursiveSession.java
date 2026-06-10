package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.List;

public class RecursiveSession {

    private static final int MAX_ITERATIONS = 30;

    private final StepRequest request;
    private final StepResolverFactory stepResolverFactory;
    private final RootHintsProvider rootHintsProvider;

    public RecursiveSession(DnsQueryRequest dnsQueryRequest, StepResolverFactory stepResolverFactory, RootHintsProvider rootHintsProvider) {
        this.request = StepRequest.fromMessage(dnsQueryRequest);
        this.stepResolverFactory = stepResolverFactory;
        this.rootHintsProvider = rootHintsProvider;
    }

    public Message resolve() {
        if (request == null) {
            return null;
        }
        StepResponse stepResponse = resolveInternal(request.qname(), request.qtype());
        if (stepResponse == null) {
            return null;
        }
        return stepResponse.toWireMessage();
    }

    private StepResponse resolveInternal(Name qname, int qtype) {
        List<NameServerAddress> authoritativeServers = resolveAuthoritativeServers(qname);
        if (authoritativeServers.isEmpty()) {
            return null;
        }
        return resolveFinalQuery(qname, qtype, authoritativeServers);
    }

    private List<NameServerAddress> resolveAuthoritativeServers(Name qname) {
        List<NameServerAddress> currentServers = rootHintsProvider.getRootServers();
        List<Name> minimizedNames = buildMinimizedNsNames(qname);

        for (Name minimizedName : minimizedNames) {
            StepRequest stepRequest = new StepRequest(minimizedName, Type.NS, request.qclass());
            currentServers = resolveNextServers(currentServers, stepRequest);
            if (currentServers.isEmpty()) {
                return List.of();
            }
        }

        return currentServers;
    }

    private StepResponse resolveFinalQuery(Name qname, int qtype, List<NameServerAddress> servers) {
        Name currentName = qname;
        List<NameServerAddress> currentServers = servers;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            StepResponse response = queryServers(currentServers, new StepRequest(currentName, qtype, request.qclass()));
            if (response == null) {
                return null;
            }

            if (response.isNXDOMAIN()) {
                return response;
            }

            if (response.hasAnswer(currentName, qtype)) {
                return response;
            }

            Name cnameTarget = response.getCnameTarget(currentName);
            if (cnameTarget != null) {
                return resolveInternal(cnameTarget, qtype);
            }

            List<NameServerAddress> referralServers = response.getReferralServers();
            if (!referralServers.isEmpty()) {
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
            return resolveNsTargets(nsTargets);
        }

        return List.of();
    }

    private StepResponse queryServers(List<NameServerAddress> servers, StepRequest request) {
        for (NameServerAddress server : servers) {
            StepResolver resolver = stepResolverFactory.create(server.ip(), server.port());
            StepResponse response = resolver.send(request);
            if (response != null) {
                return response;
            }
        }
        return null;
    }

    private List<NameServerAddress> resolveNsTargets(List<Name> targets) {
        for (Name target : targets) {
            StepResponse response = resolveInternal(target, Type.A);
            if (response == null) {
                continue;
            }
            List<NameServerAddress> addresses = response.getARecordAddresses(target);
            if (!addresses.isEmpty()) {
                return addresses;
            }
        }
        return List.of();
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
        for (int start = labels.length - 1; start >= 0; start--) {
            String joined = String.join(".", java.util.Arrays.copyOfRange(labels, start, labels.length));
            result.add(toName(joined));
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
