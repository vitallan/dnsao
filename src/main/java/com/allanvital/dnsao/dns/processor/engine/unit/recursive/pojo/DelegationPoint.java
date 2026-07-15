package com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record DelegationPoint(String zoneName, List<String> nameserverNames, List<AuthorityEndpoint> authorityEndpoints) {
}
