package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class GroupsConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "groups";
    }

    private Map<String, GroupInnerConf> getGroupsConf(String file) {
        return getConf(file).getGroups();
    }

    @Test
    public void testFullExample() {
        Map<String, GroupInnerConf> groups = getGroupsConf("full-groups-example.yml");
        assertNotNull(groups);
        assertEquals(2, groups.size());

        GroupInnerConf group1 = groups.get("group1");
        GroupInnerConf group2 = groups.get("group2");
        assertNotNull(group1);
        assertNotNull(group2);

        assertEquals(3, group1.getMembers().size());
        assertTrue(group1.getMembers().contains("192.168.68.3"));
        assertTrue(group1.getMembers().contains("192.168.68.4"));
        assertTrue(group1.getMembers().contains("192.168.68.5"));

        assertEquals(2, group1.getAllows().size());
        assertTrue(group1.getAllows().contains("allow1"));
        assertTrue(group1.getAllows().contains("allow2"));

        assertEquals(2, group1.getBlocks().size());
        assertTrue(group1.getBlocks().contains("block1"));
        assertTrue(group1.getBlocks().contains("block2"));

        assertEquals(1, group2.getMembers().size());
        assertTrue(group2.getMembers().contains("192.168.68.6"));

        assertEquals(1, group2.getAllows().size());
        assertTrue(group2.getAllows().contains("allow1"));

        assertEquals(0, group2.getBlocks().size());
    }

}
