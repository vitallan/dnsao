package com.allanvital.dnsao.dns;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.conf.inner.Upstream;
import com.allanvital.dnsao.dns.remote.resolver.UpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.doh.DOHUpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTConnectionPoolFactory;
import com.allanvital.dnsao.dns.remote.resolver.dot.DOTUpstreamResolver;
import com.allanvital.dnsao.dns.remote.resolver.udp.UdpUpstreamResolver;
import com.allanvital.dnsao.utils.ExceptionUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateParsingException;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UpstreamResolverBuilder {

    private List<UpstreamResolver> resolvers  = new LinkedList<>();

    public UpstreamResolverBuilder(DOTConnectionPoolFactory connectionPoolFactory, List<Upstream> upstreams) {
        for (Upstream upstream : upstreams) {
            String lowerCaseProtocol = upstream.getProtocol().toLowerCase();
            try {
                switch (lowerCaseProtocol) {
                    case "udp" -> this.resolvers.add(new UdpUpstreamResolver(upstream.getIp(), upstream.getPort()));
                    case "dot" -> this.resolvers.add(new DOTUpstreamResolver(connectionPoolFactory.build(upstream), upstream));
                    case "doh" -> this.resolvers.add(new DOHUpstreamResolver(upstream));
                    default -> Log.INFRA.warn("no Resolver possible for protocol {}", upstream.getProtocol());
                }
            } catch (CertificateParsingException | IOException | NoSuchAlgorithmException e) {
                Throwable rootCause = ExceptionUtils.findRootCause(e);
                Log.INFRA.error("failed to create resolver {}: {}", upstream.getIp(), rootCause.getMessage());
            }
        }
    }

    public List<UpstreamResolver> getAllResolvers() {
        return resolvers;
    }

    public void setResolvers(List<UpstreamResolver> resolvers) {
        this.resolvers = resolvers;
    }

}
