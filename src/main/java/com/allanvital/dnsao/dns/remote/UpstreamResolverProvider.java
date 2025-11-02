package com.allanvital.dnsao.dns.remote;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.doh.DOHUpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolManager;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTUpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.udp.UdpUpstreamResolver;
import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateParsingException;
import java.util.LinkedList;
import java.util.List;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamResolverProvider implements ResolverProvider {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final List<UpstreamResolver> resolvers  = new LinkedList<>();

    public UpstreamResolverProvider(DOTConnectionPoolManager dotConnectionPoolManager, List<Upstream> upstreams) {
        for (Upstream upstream : upstreams) {
            String lowerCaseProtocol = upstream.getProtocol().toLowerCase();
            try {
                switch (lowerCaseProtocol) {
                    case "udp" -> this.resolvers.add(new UdpUpstreamResolver(upstream.getIp(), upstream.getPort()));
                    case "dot" -> this.resolvers.add(new DOTUpstreamResolver(dotConnectionPoolManager, upstream));
                    case "doh" -> this.resolvers.add(new DOHUpstreamResolver(upstream));
                    default -> log.warn("no Resolver possible for protocol {}", upstream.getProtocol());
                }
            } catch (CertificateParsingException | IOException | NoSuchAlgorithmException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                log.error("failed to create resolver {}: {}", upstream.getIp(), rootCause.getMessage());
            }
        }
    }

    @Override
    public List<UpstreamResolver> getAllResolvers() {
        return resolvers;
    }

}