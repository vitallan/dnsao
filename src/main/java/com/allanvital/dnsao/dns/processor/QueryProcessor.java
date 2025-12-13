package com.allanvital.dnsao.dns.processor;

import com.allanvital.dnsao.dns.pojo.DnsQuery;
import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.processor.engine.QueryEngine;
import com.allanvital.dnsao.dns.processor.post.PostHandlerFacade;
import com.allanvital.dnsao.dns.processor.pre.PreHandlerFacade;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.net.InetAddress;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.utils.ExceptionUtils.findRootCause;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryProcessor {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    private final PreHandlerFacade preHandler;
    private final QueryEngine engine;
    private final PostHandlerFacade postHandler;

    public QueryProcessor(QueryProcessorDependencies dependencies) {
        this.preHandler = dependencies.getPreHandlerFacade();
        this.engine = dependencies.getQueryEngine();
        this.postHandler = dependencies.getPostHandlerFacade();
    }

    public DnsQuery processQuery(InetAddress clientAddress, Message clientQuery, boolean isInternalQuery) {
        DnsQueryRequest request = null;
        try {
            request = preHandler.prepare(clientAddress, clientQuery, isInternalQuery);
        } catch (PreHandlerException e) {
            log.error(e.getMessage());
            return null;
        }
        DnsQueryResponse response = engine.process(request);
        postHandler.handlePost(request, response);
        return new DnsQuery(request, response);
    }

    public DnsQuery processInternalQuery(Message message) {
        return processQuery(InetAddress.getLoopbackAddress(), message, true);
    }

    public DnsQuery processExternalQuery(InetAddress clientAddress, byte[] data) {
        Message clientQuery = buildMessage(data);
        if (clientQuery == null) {
            return null;
        }
        return this.processQuery(clientAddress, clientQuery, false);
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