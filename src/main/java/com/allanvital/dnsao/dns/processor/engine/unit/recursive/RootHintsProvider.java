package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface RootHintsProvider {

    List<AuthorityEndpoint> getRootHints();

}
