package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthorityScenarioWhenAsked {

    private final RecursiveAuthorityScenario scenario;
    private final AuthorityEndpoint authorityEndpoint;

    public RecursiveAuthorityScenarioWhenAsked(RecursiveAuthorityScenario scenario, AuthorityEndpoint authorityEndpoint) {
        this.scenario = scenario;
        this.authorityEndpoint = authorityEndpoint;
    }

    public RecursiveAuthorityScenarioQuestion forQuestion(int type, String qname) {
        return new RecursiveAuthorityScenarioQuestion(scenario, authorityEndpoint, new QuestionDefinition(type, qname));
    }
}
