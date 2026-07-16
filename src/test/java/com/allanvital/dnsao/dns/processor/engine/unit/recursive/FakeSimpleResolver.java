package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeSimpleResolver extends SimpleResolver {

    private final Message response;
    private final IOException exception;

    public FakeSimpleResolver(Message response, IOException exception) {
        super(InetAddress.getLoopbackAddress());
        this.response = response;
        this.exception = exception;
    }

    @Override
    public Message send(Message query) throws IOException {
        if (exception != null) {
            throw exception;
        }
        return response;
    }
}
