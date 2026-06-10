package com.allanvital.dnsao.dns.recursive;

public interface StepResolver {

    StepResponse send(StepRequest request);

}
