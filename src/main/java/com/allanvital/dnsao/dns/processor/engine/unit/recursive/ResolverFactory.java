package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import org.xbill.DNS.Resolver;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface ResolverFactory {

    Resolver build(AuthorityEndpoint authorityEndpoint, boolean tcp);

}
