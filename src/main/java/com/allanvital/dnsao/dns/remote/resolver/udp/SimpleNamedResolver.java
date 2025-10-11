package com.allanvital.dnsao.dns.remote.resolver.udp;

import com.allanvital.dnsao.dns.remote.resolver.NamedResolver;
import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SimpleNamedResolver extends SimpleResolver implements NamedResolver {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final String ip;
    private final int port;

    public SimpleNamedResolver(String ip, int port) throws UnknownHostException {
        super(ip);
        super.setPort(port);
        this.ip = ip;
        this.port = port;
        this.setTimeout(Duration.ofSeconds(1));
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Message send(Message query) throws IOException {
        try {
            return super.send(query);
        } catch (IOException e) {
            Record question = query.getQuestion();
            Name name = question.getName();
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                log.debug("discarding udp call to {} because other thread returned first", name);
                return null;
            }
            Throwable rootCause = ExceptionUtils.findRootCause(e);
            log.error("query to {}  failed: {}", name, rootCause.getMessage());
            return null;
        }
    }

}