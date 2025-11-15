package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class GroupsAndListsCombinedTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "groups";
    }

    @Test
    public void validateGroupsDefinitionCombinationWithLists() {
        Conf conf = getConf("full-groups-lists-check.yml");
        validatePreSanitization(conf.getGroups());
        conf.sanitizeGroups();
        validatePostSanitization(conf.getGroups());
    }

    @Test
    public void validateMain() {
        Conf conf = getConf("manual-main-set.yml");
        Map<String, GroupInnerConf> groups = conf.getGroups();
        conf.sanitizeGroups();
        assertEquals(1, groups.size());
        GroupInnerConf main = groups.get(MAIN);
        assertEquals(2, main.getAllows().size());
        assertEquals(2, main.getBlocks().size());
    }

    @Test
    public void validateEmpty() {
        Conf conf = getConf("empty.yml");
        Map<String, GroupInnerConf> groups = conf.getGroups();
        assertTrue(groups.isEmpty());
        conf.sanitizeGroups();
        GroupInnerConf main = groups.get(MAIN);
        assertNotNull(main);
        assertTrue(main.getBlocks().isEmpty());
        assertTrue(main.getAllows().isEmpty());
        assertTrue(main.getMembers().isEmpty());
    }

    private void validatePreSanitization(Map<String, GroupInnerConf> groups) {
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
        assertTrue(group1.getBlocks().contains("block4"));

        assertEquals(1, group2.getMembers().size());
        assertTrue(group2.getMembers().contains("192.168.68.6"));

        assertEquals(1, group2.getAllows().size());
        assertTrue(group2.getAllows().contains("allow5"));

        assertEquals(1, group2.getBlocks().size());
        assertTrue(group1.getBlocks().contains("block1"));
    }

    private void validatePostSanitization(Map<String, GroupInnerConf> groups) {
        assertNotNull(groups);
        assertEquals(3, groups.size());

        GroupInnerConf group1 = groups.get("group1");
        GroupInnerConf group2 = groups.get("group2");
        GroupInnerConf main = groups.get("main");
        assertNotNull(group1);
        assertNotNull(group2);
        assertNotNull(main);


        assertEquals(3, group1.getMembers().size());
        assertTrue(group1.getMembers().contains("192.168.68.3"));
        assertTrue(group1.getMembers().contains("192.168.68.4"));
        assertTrue(group1.getMembers().contains("192.168.68.5"));

        assertEquals(2, group1.getAllows().size());
        assertTrue(group1.getAllows().contains("allow1"));
        assertTrue(group1.getAllows().contains("allow2"));

        assertEquals(1, group1.getBlocks().size());
        assertTrue(group1.getBlocks().contains("block1"));


        assertEquals(1, group2.getMembers().size());
        assertTrue(group2.getMembers().contains("192.168.68.6"));

        assertEquals(0, group2.getAllows().size());

        assertEquals(1, group2.getBlocks().size());
        assertTrue(group1.getBlocks().contains("block1"));


        assertEquals(0, main.getMembers().size());

        assertEquals(2, main.getAllows().size());
        assertTrue(main.getAllows().contains("allow1"));
        assertTrue(main.getAllows().contains("allow2"));

        assertEquals(2, main.getBlocks().size());
        assertTrue(main.getBlocks().contains("block1"));
        assertTrue(main.getBlocks().contains("block2"));
    }

}
