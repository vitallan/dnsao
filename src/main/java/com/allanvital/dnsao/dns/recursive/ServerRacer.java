package com.allanvital.dnsao.dns.recursive;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServerRacer {

    private final ExecutorService executor;
    private final int timeoutSeconds;
    private final StepResolverFactory resolverFactory;
    private final DnssecDowngradeHandler dnssecHandler;

    public ServerRacer(ExecutorService executor, int timeoutSeconds, StepResolverFactory resolverFactory, DnssecDowngradeHandler dnssecHandler) {
        this.executor = executor;
        this.timeoutSeconds = timeoutSeconds;
        this.resolverFactory = resolverFactory;
        this.dnssecHandler = dnssecHandler;
    }

    public Map.Entry<NameServerAddress, StepResponse> race(List<NameServerAddress> servers, StepRequest request) {
        if (servers.isEmpty()) {
            return null;
        }
        if (servers.size() == 1) {
            return querySingleServer(servers.get(0), request);
        }

        CompletableFuture<Map.Entry<NameServerAddress, StepResponse>> raceResult = new CompletableFuture<>();
        for (NameServerAddress server : servers) {
            CompletableFuture.supplyAsync(() -> querySingleServer(server, request), executor)
                    .thenAccept(result -> {
                        if (result != null) {
                            raceResult.complete(result);
                        }
                    });
        }

        try {
            return raceResult.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    private Map.Entry<NameServerAddress, StepResponse> querySingleServer(NameServerAddress server, StepRequest request) {
        StepResolver resolver = resolverFactory.create(server.ip(), server.port());
        StepResponse response = dnssecHandler.queryWithPossibleDowngrade(resolver, request);
        if (response == null) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(server, response);
    }

}
