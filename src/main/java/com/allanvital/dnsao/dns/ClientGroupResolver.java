package com.allanvital.dnsao.dns;

import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;

import java.net.InetAddress;
import java.util.Map;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;

public class ClientGroupResolver {

    private final Map<String, GroupInnerConf> groups;

    public ClientGroupResolver(Map<String, GroupInnerConf> groups) {
        this.groups = groups;
    }

    public String resolve(InetAddress clientIp) {
        if (clientIp == null || groups == null) {
            return MAIN;
        }
        String client = clientIp.getHostAddress();
        for (Map.Entry<String, GroupInnerConf> entry : groups.entrySet()) {
            if (entry.getValue().getMembers().contains(client)) {
                return entry.getKey();
            }
        }
        return MAIN;
    }
}
