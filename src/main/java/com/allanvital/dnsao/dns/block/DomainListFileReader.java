package com.allanvital.dnsao.dns.block;

import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface DomainListFileReader {

    Set<Long> readEntries(String url);

}
