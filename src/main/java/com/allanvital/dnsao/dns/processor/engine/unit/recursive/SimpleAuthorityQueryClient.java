package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Resolver;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleAuthorityQueryClient implements AuthorityQueryClient {

    private final ResolverFactory resolverFactory;

    public SimpleAuthorityQueryClient() {
        this(new SimpleResolverFactory());
    }

    public SimpleAuthorityQueryClient(ResolverFactory resolverFactory) {
        this.resolverFactory = resolverFactory;
    }

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        try {
            Resolver udpResolver = resolverFactory.build(authorityEndpoint, false);
            Message response = udpResolver.send(query);
            if (response != null && response.getHeader().getFlag(Flags.TC)) {
                return executeTcpFallback(authorityEndpoint, query);
            }
            return AuthorityQueryResult.success(authorityEndpoint, response);
        } catch (SocketTimeoutException e) {
            return AuthorityQueryResult.timeout(authorityEndpoint, e);
        } catch (IOException e) {
            return AuthorityQueryResult.error(authorityEndpoint, e);
        }
    }

    private AuthorityQueryResult executeTcpFallback(AuthorityEndpoint authorityEndpoint, Message query) {
        try {
            Resolver tcpResolver = resolverFactory.build(authorityEndpoint, true);
            Message response = tcpResolver.send(query);
            return AuthorityQueryResult.success(authorityEndpoint, response);
        } catch (SocketTimeoutException e) {
            return AuthorityQueryResult.timeout(authorityEndpoint, e);
        } catch (IOException e) {
            return AuthorityQueryResult.error(authorityEndpoint, e);
        }
    }
}
