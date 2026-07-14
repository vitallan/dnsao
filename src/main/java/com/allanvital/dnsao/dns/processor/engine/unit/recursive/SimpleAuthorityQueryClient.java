package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleAuthorityQueryClient implements AuthorityQueryClient {

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        try {
            SimpleResolver resolver = new SimpleResolver(authorityEndpoint.address());
            resolver.setPort(authorityEndpoint.port());
            resolver.setTCP(false);
            Message response = resolver.send(query);
            if (response != null && response.getHeader().getFlag(Flags.TC)) {
                return AuthorityQueryResult.truncated(authorityEndpoint, response);
            }
            return AuthorityQueryResult.success(authorityEndpoint, response);
        } catch (SocketTimeoutException e) {
            return AuthorityQueryResult.timeout(authorityEndpoint, e);
        } catch (IOException e) {
            return AuthorityQueryResult.error(authorityEndpoint, e);
        }
    }
}
