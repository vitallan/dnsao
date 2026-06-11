package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record RecursiveTransportServerHistories(
        List<DnsQueryKey> primaryQueries,
        List<DnsQueryKey> secondaryUdpQueries,
        List<DnsQueryKey> secondaryTcpQueries
) {
}
