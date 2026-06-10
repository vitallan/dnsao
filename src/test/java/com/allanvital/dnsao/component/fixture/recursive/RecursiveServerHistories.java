package com.allanvital.dnsao.component.fixture.recursive;

import com.allanvital.dnsao.graph.bean.DnsQueryKey;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record RecursiveServerHistories(
        List<DnsQueryKey> primaryQueries,
        List<DnsQueryKey> secondaryQueries,
        List<DnsQueryKey> tertiaryQueries
) {
}
