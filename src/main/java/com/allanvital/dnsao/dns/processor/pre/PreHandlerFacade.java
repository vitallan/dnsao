package com.allanvital.dnsao.dns.processor.pre;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.pre.handler.PreHandler;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.utils.ExceptionUtils.findRootCause;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class PreHandlerFacade {

    private static final Logger log = LoggerFactory.getLogger(DNS);
    private final List<PreHandler> handlers;

    public PreHandlerFacade(PreHandlerProvider preHandlerProvider) {
        this.handlers = preHandlerProvider.getOrderedPreHandlers();
    }

    public DnsQueryRequest prepare(InetAddress clientAddress, byte[] request, boolean isInternalQuery) throws PreHandlerException {
        DnsQueryRequest dnsQueryRequest = new DnsQueryRequest(clientAddress);
        Message clientQuery = buildMessage(request);
        if (clientQuery == null) {
            throw new PreHandlerException("It was not possible to parse the clients query from " + clientAddress);
        }
        dnsQueryRequest.setOriginalRequest(clientQuery);
        Message internalQuery = clientQuery.clone();
        for (PreHandler preHandler : handlers) {
            internalQuery = preHandler.prepare(internalQuery);
        }
        dnsQueryRequest.setRequest(internalQuery);
        dnsQueryRequest.setIsLocalQuery(isInternalQuery);
        return dnsQueryRequest;
    }

    private Message buildMessage(byte[] rawMessage) {
        Message clientQuery = null;
        try {
            clientQuery = new Message(rawMessage);
        } catch (IOException e) {
            Throwable rootCause = findRootCause(e);
            log.error("Failed to build query for processing: {} - {}", rootCause, rootCause.getMessage());
        }
        return clientQuery;
    }

}
