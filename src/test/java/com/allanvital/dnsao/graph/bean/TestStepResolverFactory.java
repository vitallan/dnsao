package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.recursive.StepResolver;
import com.allanvital.dnsao.dns.recursive.StepResolverFactory;

import static com.allanvital.dnsao.Constants.DEFAULT_DNS_PORT;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestStepResolverFactory extends StepResolverFactory {

    private int portToUse = DEFAULT_DNS_PORT;

    public TestStepResolverFactory(int timeoutMs) {
        super(timeoutMs);
    }

    public void setPortToUse(int port) {
        this.portToUse = port;
    }

    public int getPortToUse() {
        return portToUse;
    }

    @Override
    public StepResolver create(String ip, int port) {
        return super.create(ip, portToUse);
    }

}
