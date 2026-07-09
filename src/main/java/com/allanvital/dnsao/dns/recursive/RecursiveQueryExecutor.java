package com.allanvital.dnsao.dns.recursive;

import java.util.List;
import java.util.Map;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class RecursiveQueryExecutor {

    private final RecursiveCache recursiveCache;
    private final ServerRacer serverRacer;
    private final RecursiveSessionState sessionState;

    RecursiveQueryExecutor(RecursiveCache recursiveCache, ServerRacer serverRacer, RecursiveSessionState sessionState) {
        this.recursiveCache = recursiveCache;
        this.serverRacer = serverRacer;
        this.sessionState = sessionState;
    }

    StepResponse queryServers(List<NameServerAddress> servers, StepRequest request) {
        if (!sessionState.hasRemainingSessionBudget()) {
            return null;
        }
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

    Map.Entry<NameServerAddress, StepResponse> queryServersFresh(List<NameServerAddress> servers, StepRequest request) {
        if (!sessionState.hasRemainingSessionBudget()) {
            return null;
        }
        return serverRacer.race(servers, request);
    }

    void cacheResponse(StepRequest request, StepResponse response) {
        recursiveCache.put(request, response);
    }

    List<NameServerAddress> resolveNavigationServers(List<NameServerAddress> currentServers, StepRequest request) {
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
}
