package com.allanvital.dnsao.dns.recursive;

public class StepResolverFactory {

    private final int timeoutMs;
    private final RecursiveStatsCollector recursiveStatsCollector;

    public StepResolverFactory(int timeoutMs) {
        this(timeoutMs, new NoOpRecursiveStatsCollector());
    }

    public StepResolverFactory(int timeoutMs, RecursiveStatsCollector recursiveStatsCollector) {
        this.timeoutMs = timeoutMs;
        this.recursiveStatsCollector = recursiveStatsCollector;
    }

    public StepResolver create(String ip, int port) {
        return new UdpStepResolver(ip, port, timeoutMs, recursiveStatsCollector);
    }

}
