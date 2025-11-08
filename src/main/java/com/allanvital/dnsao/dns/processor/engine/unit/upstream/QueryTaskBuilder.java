package com.allanvital.dnsao.dns.processor.engine.unit.upstream;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.engine.pojo.DnsQueryResult;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.exc.DnsSecPolicyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.RIGID;
import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class QueryTaskBuilder {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    public static Callable<DnsQueryResult> buildQueryTask(UpstreamResolver resolver, Message query, DNSSecMode dnsSecMode) {
        return new Callable<DnsQueryResult>() {
            @Override
            public DnsQueryResult call() throws Exception {
                Message response = resolver.send(query);
                if (response == null) {
                    throw new IOException("Null response");
                }
                if (shouldRejectByDnssecPolicy(response, dnsSecMode)) {
                    throw new DnsSecPolicyException("non accepted answer based on dnssec policy");
                }
                return new DnsQueryResult(response, resolver);
            }
        };
    }

    private static boolean shouldRejectByDnssecPolicy(Message response, DNSSecMode dnsSecMode) {
        final boolean ad = response.getHeader().getFlag(Flags.AD);
        if (RIGID.equals(dnsSecMode)) {
            return !ad;
        }
        return false;
    }

}
