package com.allanvital.dnsao.dns.processor.post;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.post.handler.PostHandler;
import com.allanvital.dnsao.graph.ExecutorServiceFactory;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;

import java.util.concurrent.ExecutorService;

import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PostHandlerFacade {

    private final PostHandlerProvider provider;
    private final ExecutorService threadPool;

    public PostHandlerFacade(PostHandlerProvider provider, ExecutorServiceFactory executorServiceFactory) {
        this.provider = provider;
        threadPool = executorServiceFactory.buildExecutor("post-handler", 5);
    }

    public void handlePost(DnsQueryRequest request, DnsQueryResponse response) {
        if (response == null) {
            Log.DNS.error("the following request resulted in a null response: ");
            Log.DNS.error(request.getRequest().toString());
            //should never happen
            return;
        }
        response.markFinishTime();
        for (PostHandler postHandler : provider.getPostHandlers()) {
            threadPool.execute(() -> postHandler.handle(request, response));
        }
        telemetryNotify(EventType.QUERY_RESOLVED);
    }

}
