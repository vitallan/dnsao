package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
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

        for (int i = 0; i < minimizedNames.size(); i++) {
            Name minimizedName = minimizedNames.get(i);
            StepRequest stepRequest = new StepRequest(minimizedName, Type.NS, request.qclass());
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

            if (response.isNoDataFor(currentName, qtype)) {
                return response;
            }

            Name cnameTarget = response.getCnameTarget(currentName);
            if (cnameTarget != null) {
                StepResponse targetResponse = resolveInternal(cnameTarget, qtype);
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
        Map.Entry<NameServerAddress, StepResponse> responseEntry = queryServersWithSource(currentServers, request);
        if (responseEntry == null || responseEntry.getValue().isNXDOMAIN()) {
            return List.of();
        }
        NameServerAddress respondingServer = responseEntry.getKey();
        return List.of(new NameServerAddress(respondingServer.ip(), respondingServer.port()));
    }

    private StepResponse queryServers(List<NameServerAddress> servers, StepRequest request) {
        Map.Entry<NameServerAddress, StepResponse> responseEntry = queryServersWithSource(servers, request);
        if (responseEntry == null) {
            return null;
        }
        return responseEntry.getValue();
    }

    private Map.Entry<NameServerAddress, StepResponse> queryServersWithSource(List<NameServerAddress> servers, StepRequest request) {
        for (NameServerAddress server : servers) {
            StepResolver resolver = stepResolverFactory.create(server.ip(), server.port());
            StepResponse response = resolver.send(request);
            if (response != null) {
                return new AbstractMap.SimpleEntry<>(server, response);
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
