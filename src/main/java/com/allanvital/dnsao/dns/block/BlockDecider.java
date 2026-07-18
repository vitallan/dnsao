package com.allanvital.dnsao.dns.block;

import com.allanvital.dnsao.conf.inner.ListsConf;
import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import com.allanvital.dnsao.dns.ClientGroupResolver;
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
    private final ClientGroupResolver clientGroupResolver;

    public BlockDecider(FileListsProvider fileListsProvider, ListsConf listsConf, Map<String, GroupInnerConf> groups) {
        this.fileListsProvider = fileListsProvider;
        this.listsConf = listsConf;
        this.groups = groups;
        this.clientGroupResolver = new ClientGroupResolver(groups);
    }

    public boolean isBlocked(InetAddress clientIp, Name name) {
        String clientGroup = clientGroupResolver.resolve(clientIp);
        Set<String> allowedNames = getAllowedNames(clientGroup);
        Set<String> blockedNames = getBlockedNames(clientGroup);
        if (fileListsProvider.isInAllowed(name, allowedNames)) {
            return false;
        }
        return fileListsProvider.isInBlocked(name, blockedNames);
    }

    private Set<String> getAllowedNames(String group) {
        GroupInnerConf groupConf = groups.get(group);
        if (groupConf != null) {
            return groupConf.getAllows();
        } else if (MAIN.equals(group)) {
            return listsConf.getValidAllowNames();
        } else {
            return Set.of();
        }
    }

    private Set<String> getBlockedNames(String group) {
        GroupInnerConf groupConf = groups.get(group);
        if (groupConf != null) {
            return groupConf.getBlocks();
        } else if (MAIN.equals(group)) {
            return listsConf.getValidBlockNames();
        } else {
            return Set.of();
        }
    }

}
