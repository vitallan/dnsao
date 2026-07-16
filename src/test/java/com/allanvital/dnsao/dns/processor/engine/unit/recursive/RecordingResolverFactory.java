package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import org.xbill.DNS.Resolver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecordingResolverFactory implements ResolverFactory {

    private final LinkedList<FakeSimpleResolver> resolvers;
    private final List<Boolean> tcpModes = new ArrayList<>();

    public RecordingResolverFactory(List<FakeSimpleResolver> resolvers) {
        this.resolvers = new LinkedList<>(resolvers);
    }

    @Override
    public Resolver build(AuthorityEndpoint authorityEndpoint, boolean tcp) {
        tcpModes.add(tcp);
        return resolvers.removeFirst();
    }

    public List<Boolean> getTcpModes() {
        return tcpModes;
    }
}
