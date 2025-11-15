package com.allanvital.dnsao.dns.block;

import com.allanvital.dnsao.conf.inner.ListsConf;
import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import org.xbill.DNS.Name;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockDecider {

    private final FileListsProvider fileListsProvider;
    private final ListsConf listsConf;
    private final Map<String, GroupInnerConf> groups;

    public BlockDecider(FileListsProvider fileListsProvider, ListsConf listsConf, Map<String, GroupInnerConf> groups) {
        this.fileListsProvider = fileListsProvider;
        this.listsConf = listsConf;
        this.groups = groups;
    }

    public boolean isBlocked(InetAddress clientIp, Name name) {
        String clientGroup = getClientGroup(clientIp.getHostAddress());
        Set<String> allowedNames = getAllowedNames(clientGroup);
        Set<String> blockedNames = getBlockedNames(clientGroup);
        if (fileListsProvider.isInAllowed(name, allowedNames)) {
            return false;
        }
        return fileListsProvider.isInBlocked(name, blockedNames);
    }

    private String getClientGroup(String client) {
        for (Map.Entry<String, GroupInnerConf> entry : groups.entrySet()) {
            if (entry.getValue().getMembers().contains(client)) {
                return entry.getKey();
            }
        }
        return MAIN;
    }

    private Set<String> getAllowedNames(String group) {
        if (MAIN.equals(group)) {
            return listsConf.getValidAllowNames();
        }
        return groups.get(group).getAllows();
    }

    private Set<String> getBlockedNames(String group) {
        if (MAIN.equals(group)) {
            return listsConf.getValidBlockNames();
        }
        return groups.get(group).getBlocks();
    }

}
