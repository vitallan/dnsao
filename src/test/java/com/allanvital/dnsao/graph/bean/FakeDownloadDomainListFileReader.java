package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.block.DomainListFileReader;
import com.allanvital.dnsao.utils.HashUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeDownloadDomainListFileReader implements DomainListFileReader {

    private final Map<String, List<String>> map = new HashMap<>();
    private int readEntriesCallCount = 0;

    public FakeDownloadDomainListFileReader() {
        map.put("urlAllow1", List.of("allow1.com", "conflict.com"));
        map.put("urlAllow2", List.of("allow2.com"));

        map.put("urlBlock1", List.of("block1.com", "conflict.com"));
        map.put("urlBlock2", List.of("block2.com"));
    }

    @Override
    public Set<Long> readEntries(String url) {
        readEntriesCallCount++;
        return map.get(url).stream().map(HashUtils::fnv1a64).collect(Collectors.toSet());
    }

    public int getReadEntriesCallCount() {
        return readEntriesCallCount;
    }

}
