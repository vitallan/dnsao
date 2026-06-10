package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;

public class UdpStepResolver implements StepResolver {

    private final SimpleResolver resolver;

    public UdpStepResolver(String ip, int port, int timeoutMs) {
        try {
            this.resolver = new SimpleResolver(ip);
            this.resolver.setPort(port);
            this.resolver.setTimeout(Duration.ofMillis(timeoutMs));
            this.resolver.setTCP(false);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("unknown host: " + ip, e);
        }
    }

    @Override
    public StepResponse send(StepRequest request) {
        Message query = request.toWireMessage();
        try {
            Message response = resolver.send(query);
            if (response == null) {
                return null;
            }
            return new StepResponse(response);
        } catch (IOException e) {
            return null;
        }
    }

}
