package com.allanvital.dnsao.dns.processor.post.handler;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.InetAddress;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;
import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogPostHandler implements PostHandler {

    protected static final Logger log = LoggerFactory.getLogger(DNS);

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        InetAddress clientAddress = request.getClientAddress();
        Record question = request.getRequest().getQuestion();
        Name name = question.getName();
        int type = question.getType();
        String typeName = Type.string(type);
        String client = clientAddress.getHostAddress();

        if (UPSTREAM.equals(response.getQueryResolvedBy())) {
            log.debug("Query {} from {} to {} solved by {}", typeName, client, name, response.getResponseSource());
        } else {
            log.debug("Query {} from {} to {} solved by {}", typeName, client, name, response.getQueryResolvedBy());
        }

    }

}
