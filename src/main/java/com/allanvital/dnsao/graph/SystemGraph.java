package com.allanvital.dnsao.graph;

import com.allanvital.dnsao.conf.Conf;
import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.exc.ConfException;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class SystemGraph {

    private final SystemGraphAssembler systemGraphAssembler = new SystemGraphAssembler();
    private final DnsServer dnsServer;

    public SystemGraph(Conf conf) throws ConfException {
        dnsServer = systemGraphAssembler.assemble(conf);
    }

    public void start() {
        dnsServer.start();
    }

    public void stop() {
        if (dnsServer != null) {
            dnsServer.stop();
        }
    }

    public DnsServer getDnsServer() {
        return dnsServer;
    }

}
