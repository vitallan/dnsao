package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;

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
        StepResponse stepResponse = resolveInternal();
        if (stepResponse == null) {
            return null;
        }
        return stepResponse.toWireMessage();
    }

    private StepResponse resolveInternal() {
        Name qname = request.qname();
        int qtype = request.qtype();

        List<NameServerAddress> currentServers = rootHintsProvider.getRootServers();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            for (NameServerAddress server : currentServers) {
                StepResolver resolver = stepResolverFactory.create(server.ip(), server.port());
                StepResponse response = resolver.send(new StepRequest(qname, qtype, request.qclass()));

                if (response == null) {
                    continue;
                }

                if (response.isNXDOMAIN()) {
                    return response;
                }

                if (response.hasAnswer(qname, qtype)) {
                    return response;
                }

                Name cnameTarget = response.getCnameTarget(qname);
                if (cnameTarget != null) {
                    qname = cnameTarget;
                    currentServers = rootHintsProvider.getRootServers();
                    break;
                }

                List<NameServerAddress> referralServers = response.getReferralServers();
                if (!referralServers.isEmpty()) {
                    currentServers = referralServers;
                    break;
                }
            }
        }

        return null;
    }

}
