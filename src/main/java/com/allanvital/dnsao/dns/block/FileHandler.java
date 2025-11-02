package com.allanvital.dnsao.dns.block;

import java.util.List;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface FileHandler {

    void downloadFiles(List<String> urls, ListType type);
    Set<Long> readAllEntriesOfType(ListType type);

}
