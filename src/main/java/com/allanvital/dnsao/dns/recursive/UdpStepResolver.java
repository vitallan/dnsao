package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class UdpStepResolver implements StepResolver {

    private final String ip;
    private final int port;
    private final int timeoutMs;

    public UdpStepResolver(String ip, int port, int timeoutMs) {
        this.ip = ip;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public StepResponse send(StepRequest request) {
        Message query = request.toWireMessage();
        try {
            SimpleResolver udpResolver = buildResolver(false);
            Message response = udpResolver.send(query);
            if (response == null) {
                return null;
            }
            StepResponse stepResponse = new StepResponse(response);
            if (!stepResponse.isTruncated()) {
                return stepResponse;
            }

            SimpleResolver tcpResolver = buildResolver(true);
            Message tcpResponse = tcpResolver.send(query);
            if (tcpResponse == null) {
                return null;
            }
            return new StepResponse(tcpResponse);
        } catch (IOException e) {
            return null;
        }
    }

    private SimpleResolver buildResolver(boolean tcp) {
        try {
            SimpleResolver resolver = new SimpleResolver(ip);
            resolver.setPort(port);
            resolver.setTimeout(Duration.ofSeconds(timeoutMs));
            resolver.setTCP(tcp);
            return resolver;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("unknown host: " + ip, e);
        }
    }

}
