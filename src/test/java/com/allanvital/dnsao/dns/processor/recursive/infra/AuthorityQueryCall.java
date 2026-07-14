package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public record AuthorityQueryCall(AuthorityEndpoint authorityEndpoint, Message query) {
}
