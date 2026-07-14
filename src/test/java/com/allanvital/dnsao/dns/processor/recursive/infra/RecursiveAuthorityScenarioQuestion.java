package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthorityScenarioQuestion {

    private final RecursiveAuthorityScenario scenario;
    private final AuthorityEndpoint authorityEndpoint;
    private final QuestionDefinition questionDefinition;

    public RecursiveAuthorityScenarioQuestion(RecursiveAuthorityScenario scenario,
                                              AuthorityEndpoint authorityEndpoint,
                                              QuestionDefinition questionDefinition) {
        this.scenario = scenario;
        this.authorityEndpoint = authorityEndpoint;
        this.questionDefinition = questionDefinition;
    }

    public void returnReferral(ReferralAnswer referralAnswer) {
        scenario.addEntry(new RecursiveAuthorityScenarioEntry(authorityEndpoint, questionDefinition, referralAnswer, null));
    }

    public void returnAnswer(PositiveAnswer positiveAnswer) {
        scenario.addEntry(new RecursiveAuthorityScenarioEntry(authorityEndpoint, questionDefinition, null, positiveAnswer));
    }
}
