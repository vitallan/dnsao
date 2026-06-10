package com.allanvital.dnsao.dns.recursive;

public class StepResolverFactory {

    private final int timeoutMs;

    public StepResolverFactory(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public StepResolver create(String ip, int port) {
        return new UdpStepResolver(ip, port, timeoutMs);
    }

}
