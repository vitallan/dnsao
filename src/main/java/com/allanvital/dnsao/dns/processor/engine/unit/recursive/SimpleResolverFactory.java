package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleResolverFactory implements ResolverFactory {

    @Override
    public Resolver build(AuthorityEndpoint authorityEndpoint, boolean tcp) {
        SimpleResolver resolver = new SimpleResolver(authorityEndpoint.address());
        resolver.setPort(authorityEndpoint.port());
        resolver.setTCP(tcp);
        return resolver;
    }
}
