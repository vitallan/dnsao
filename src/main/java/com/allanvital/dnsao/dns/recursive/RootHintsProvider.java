package com.allanvital.dnsao.dns.recursive;

import java.util.List;

public class RootHintsProvider {

    private static final List<NameServerAddress> ROOT_SERVERS = List.of(
            new NameServerAddress("198.41.0.4", 53),
            new NameServerAddress("199.9.14.201", 53),
            new NameServerAddress("192.33.4.12", 53),
            new NameServerAddress("199.7.91.13", 53),
            new NameServerAddress("192.203.230.10", 53),
            new NameServerAddress("192.5.5.241", 53),
            new NameServerAddress("192.112.36.4", 53),
            new NameServerAddress("198.97.190.53", 53),
            new NameServerAddress("192.36.148.17", 53),
            new NameServerAddress("192.58.128.30", 53),
            new NameServerAddress("193.0.14.129", 53),
            new NameServerAddress("199.7.83.42", 53),
            new NameServerAddress("202.12.27.33", 53)
    );

    public List<NameServerAddress> getRootServers() {
        return ROOT_SERVERS;
    }

}
