package com.allanvital.dnsao.conf.inner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ListsConf {

    private Map<String, String> allowLists = new HashMap<>();
    private Map<String, String> blockLists = new HashMap<>();

    public Map<String, String> getAllowLists() {
        return allowLists;
    }

    public void setAllowLists(Map<String, String> allowLists) {
        this.allowLists = allowLists;
    }

    public Map<String, String> getBlockLists() {
        return blockLists;
    }

    public void setBlockLists(Map<String, String> blockLists) {
        this.blockLists = blockLists;
    }

    public Set<String> getValidAllowNames() {
        return getValidNames(allowLists);
    }

    public Set<String> getValidBlockNames() {
        return getValidNames(blockLists);
    }

    private Set<String> getValidNames(Map<String, String> map) {
        Set<String> toReturn = new HashSet<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            toReturn.add(entry.getKey());
        }
        return toReturn;

    }

}
