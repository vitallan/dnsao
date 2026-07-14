package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveAuthorityScenarioEntry {

    private final AuthorityEndpoint authorityEndpoint;
    private final QuestionDefinition questionDefinition;
    private final ReferralAnswer referralAnswer;
    private final PositiveAnswer positiveAnswer;

    public RecursiveAuthorityScenarioEntry(AuthorityEndpoint authorityEndpoint,
                                          QuestionDefinition questionDefinition,
                                          ReferralAnswer referralAnswer,
                                          PositiveAnswer positiveAnswer) {
        this.authorityEndpoint = authorityEndpoint;
        this.questionDefinition = questionDefinition;
        this.referralAnswer = referralAnswer;
        this.positiveAnswer = positiveAnswer;
    }

    public AuthorityEndpoint getAuthorityEndpoint() {
        return authorityEndpoint;
    }

    public QuestionDefinition getQuestionDefinition() {
        return questionDefinition;
    }

    public ReferralAnswer getReferralAnswer() {
        return referralAnswer;
    }

    public PositiveAnswer getPositiveAnswer() {
        return positiveAnswer;
    }
}
