package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTNamedResolver;
import com.allanvital.dnsao.dns.remote.resolver.udp.SimpleNamedResolver;
import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ResolverFactory {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final List<NamedResolver> resolvers  = new LinkedList<>();

    public ResolverFactory(DOTConnectionPoolManager dotConnectionPoolManager, List<Upstream> upstreams) {
        for (Upstream upstream : upstreams) {
            try {
                if ("udp".equalsIgnoreCase(upstream.getProtocol())) {
                    this.resolvers.add(new SimpleNamedResolver(upstream.getIp(), upstream.getPort()));
                } else if ("dot".equalsIgnoreCase(upstream.getProtocol())) {
                    this.resolvers.add(new DOTNamedResolver(dotConnectionPoolManager, upstream));
                } else {
                    log.warn("no Resolver possible for protocol {}", upstream.getProtocol());
                }
            } catch (CertificateParsingException | IOException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                log.error("failed to create resolver {}: {}", upstream.getIp(), rootCause.getMessage());
            }
        }
    }

    public NamedResolver getResolver() {
        Random r = new Random();
        int low = 0;
        int high = resolvers.size();
        return resolvers.get(r.nextInt(high-low) + low);
    }

    public List<NamedResolver> getAllResolvers() {
        return resolvers;
    }

}