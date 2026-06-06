package com.allanvital.dnsao.dns.processor.pre;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.pre.handler.PreHandler;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static com.allanvital.dnsao.utils.ExceptionUtils.findRootCause;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PreHandlerFacade {

    private final List<PreHandler> handlers;

    public PreHandlerFacade(PreHandlerProvider preHandlerProvider) {
        this.handlers = preHandlerProvider.getOrderedPreHandlers();
    }

    public DnsQueryRequest prepare(InetAddress clientAddress, Message request, boolean isInternalQuery) throws PreHandlerException {
        DnsQueryRequest dnsQueryRequest = new DnsQueryRequest(clientAddress);
        dnsQueryRequest.setOriginalRequest(request);
        Message internalQuery = request.clone();
        for (PreHandler preHandler : handlers) {
            internalQuery = preHandler.prepare(internalQuery);
        }
        dnsQueryRequest.setRequest(internalQuery);
        dnsQueryRequest.setIsLocalQuery(isInternalQuery);
        return dnsQueryRequest;
    }

}
