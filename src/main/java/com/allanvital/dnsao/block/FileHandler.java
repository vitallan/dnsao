package com.allanvital.dnsao.block;

import java.util.List;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface FileHandler {

    void downloadFiles(List<String> urls, ListType type);
    Set<String> readAllEntriesOfType(ListType type);

}
