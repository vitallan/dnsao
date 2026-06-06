package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.block.DomainListFileReader;
import com.allanvital.dnsao.utils.HashUtils;

import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CyclingReader implements DomainListFileReader {

    private long counter = 0;

    @Override
    public Set<Long> readEntries(String url) {
        counter++;
        return Set.of(HashUtils.fnv1a64("domain-" + counter + ".com"));
    }

}
