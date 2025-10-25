package com.allanvital.dnsao.dns.local;

import com.allanvital.dnsao.block.BlockListProvider;
import com.allanvital.dnsao.dns.remote.pojo.DnsQuery;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static com.allanvital.dnsao.dns.remote.DnsUtils.buildBlocked;
import static com.allanvital.dnsao.dns.remote.DnsUtils.buildLocalResponse;
import static com.allanvital.dnsao.notification.QueryResolvedBy.BLOCKED;
import static com.allanvital.dnsao.notification.QueryResolvedBy.LOCAL;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LocalResolver {

    private final BlockListProvider blockListProvider;
    private final Map<String, String> localMappings;

    public LocalResolver(BlockListProvider blockListProvider, Map<String, String> localMappings) {
        this.blockListProvider = blockListProvider;
        this.localMappings = localMappings;
    }

    public DnsQuery resolve(InetAddress clientAddress, Message query) throws UnknownHostException, TextParseException {
        Record question = query.getQuestion();
        Name name = question.getName();
        DnsQuery dnsQuery = null;
        if (blockListProvider != null && blockListProvider.isBlocked(name)) {
            dnsQuery = new DnsQuery(clientAddress.getHostAddress());
            Message response = buildBlocked(query);
            dnsQuery.setResolvedByAndResponse(BLOCKED, response);
            return dnsQuery;
        } else if (localMappings.containsKey(name.toString().toLowerCase())) {
            dnsQuery = new DnsQuery(clientAddress.getHostAddress());
            String ip = localMappings.get(name.toString().toLowerCase());
            Message localAnswer = buildLocalResponse(query, ip);
            dnsQuery.setResolvedByAndResponse(LOCAL, localAnswer);
        }
        return dnsQuery;
    }

}
