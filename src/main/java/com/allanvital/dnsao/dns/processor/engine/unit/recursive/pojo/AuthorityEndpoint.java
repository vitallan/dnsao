package com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo;

import java.net.InetAddress;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record AuthorityEndpoint(String name, InetAddress address, int port) {
}
