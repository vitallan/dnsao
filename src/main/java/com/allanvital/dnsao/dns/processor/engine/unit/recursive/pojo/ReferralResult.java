package com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo;

import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ReferralResult {

    public enum Type {
        FINAL_ANSWER,
        REFERRAL,
        UNUSABLE
    }

    private final Type type;
    private final Message finalAnswer;
    private final DelegationPoint delegationPoint;

    private ReferralResult(Type type, Message finalAnswer, DelegationPoint delegationPoint) {
        this.type = type;
        this.finalAnswer = finalAnswer;
        this.delegationPoint = delegationPoint;
    }

    public static ReferralResult finalAnswer(Message answer) {
        return new ReferralResult(Type.FINAL_ANSWER, answer, null);
    }

    public static ReferralResult referral(DelegationPoint delegationPoint) {
        return new ReferralResult(Type.REFERRAL, null, delegationPoint);
    }

    public static ReferralResult unusable() {
        return new ReferralResult(Type.UNUSABLE, null, null);
    }

    public Type getType() {
        return type;
    }

    public Message getFinalAnswer() {
        return finalAnswer;
    }

    public DelegationPoint getDelegationPoint() {
        return delegationPoint;
    }
}
