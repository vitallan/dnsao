package com.allanvital.dnsao.cache.keep;

import com.allanvital.dnsao.cache.pojo.KeepEntry;
import com.allanvital.dnsao.dns.processor.QueryProcessor;
import com.allanvital.dnsao.dns.processor.QueryProcessorFactory;
import org.xbill.DNS.Message;

import java.net.InetAddress;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class KeepKickstarter {

    private final KeepProvider keepProvider;
    private final QueryProcessorFactory queryProcessorFactory;

    public KeepKickstarter (KeepProvider keepProvider,
                            QueryProcessorFactory queryProcessorFactory) {

        this.keepProvider = keepProvider;
        this.queryProcessorFactory = queryProcessorFactory;
    }

    public void kickStartKeep() {
        List<KeepEntry> urlsToKeep = keepProvider.getUrlsToKeep();
        for (KeepEntry entry : urlsToKeep) {
            Message query = Message.newQuery(entry.getRecord());
            QueryProcessor queryProcessor = this.queryProcessorFactory.buildQueryProcessor();
            queryProcessor.processExternalQuery(InetAddress.getLoopbackAddress(), query.toWire());
        }
    }

}
