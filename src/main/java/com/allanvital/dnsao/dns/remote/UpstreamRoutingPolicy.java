package com.allanvital.dnsao.dns.remote;

public record UpstreamRoutingPolicy(String group) {

    public static UpstreamRoutingPolicy forGroup(String group) {
        return new UpstreamRoutingPolicy(group);
    }
}
