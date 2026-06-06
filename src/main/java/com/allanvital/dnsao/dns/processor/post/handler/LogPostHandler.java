package com.allanvital.dnsao.dns.processor.post.handler;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.pojo.DnsQueryResponse;
import com.allanvital.dnsao.dns.remote.DnsUtils;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.net.InetAddress;

import static com.allanvital.dnsao.infra.notification.QueryResolvedBy.UPSTREAM;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogPostHandler implements PostHandler {


    private final boolean queryLogEnabled;

    public LogPostHandler(boolean queryLogEnabled) {
        this.queryLogEnabled = queryLogEnabled;
    }

    @Override
    public void handle(DnsQueryRequest request, DnsQueryResponse response) {
        if (!queryLogEnabled) {
            return;
        }
        InetAddress clientAddress = request.getClientAddress();
        Record question = request.getRequest().getQuestion();
        Name name = question.getName();
        int type = question.getType();
        String typeName = Type.string(type);
        String client = clientAddress.getHostAddress();
        String solvedBy = response.getQueryResolvedBy().name();
        if (UPSTREAM.equals(response.getQueryResolvedBy())) {
            solvedBy = response.getResponseSource();
        }
        String ip = DnsUtils.extractIpFromResponseMessage(response.getResponse());
        double elapsedMs = (response.getFinishTime() - request.getStart()) / 1_000_000.0;
        String elapsedStr = String.format("%.4f", elapsedMs);
        Log.DNS.info("query:\"{}\" from:\"{}\" to:\"{}\" solved_by:\"{}\" response:\"{}\" elapsed_ms:\"{}\"",
            typeName, client, name, solvedBy, ip, elapsedStr);
    }

}
