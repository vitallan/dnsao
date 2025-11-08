package com.allanvital.dnsao.dns.processor.engine.unit.upstream;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TaskProvider {

    private final List<UpstreamResolver> resolvers;
    private final DNSSecMode dnsSecMode;

    public TaskProvider(List<UpstreamResolver> resolvers, DNSSecMode dnsSecMode) {
        this.resolvers = resolvers;
        this.dnsSecMode = dnsSecMode;
    }

    public List<Callable<DnsQueryResult>> buildTasksToExecute(Message query) {
        List<Callable<DnsQueryResult>> tasks = new LinkedList<>();
        for (UpstreamResolver resolver : resolvers) {
            tasks.add(QueryTaskBuilder.buildQueryTask(resolver, query, dnsSecMode));
        }
        return tasks;
    }

}
