package com.allanvital.dnsao.conf.inner.pojo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class GroupInnerConf {

    public static final String MAIN = "main"; //catchall group

    private List<String> members = new LinkedList<>();
    private Set<String> allows = new HashSet<>();
    private Set<String> blocks = new HashSet<>();

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public Set<String> getAllows() {
        if (allows == null) {
            allows = new HashSet<>();
        }
        return allows;
    }

    public void setAllows(Set<String> allows) {
        this.allows = allows;
    }

    public Set<String> getBlocks() {
        if (blocks == null) {
            blocks = new HashSet<>();
        }
        return blocks;
    }

    public void setBlocks(Set<String> blocks) {
        this.blocks = blocks;
    }

}
