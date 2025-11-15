package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.ListsConf;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ListsConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "lists";
    }

    private ListsConf getListsConf(String file) {
        return getConf(file).getLists();
    }

    @Test
    public void assertEmptyListsFile() {
        ListsConf listsConf = getListsConf("empty.yml");
        assertNotNull(listsConf.getAllowLists());
        assertNotNull(listsConf.getBlockLists());
        assertTrue(listsConf.getAllowLists().isEmpty());
        assertTrue(listsConf.getBlockLists().isEmpty());
    }

    @Test
    public void assertBothAllowAndBlocklist() {
        ListsConf listsConf = getListsConf("block-allow-list.yml");
        assertNotNull(listsConf.getAllowLists());
        assertNotNull(listsConf.getBlockLists());

        Map<String, String> allowMap = listsConf.getAllowLists();
        Map<String, String> blockMap = listsConf.getBlockLists();

        assertFalse(allowMap.isEmpty());
        assertFalse(blockMap.isEmpty());

        assertEquals("url1", allowMap.get("allow1"));
        assertEquals("url2", allowMap.get("allow2"));

        assertEquals("url3", blockMap.get("block1"));
        assertEquals("url4", blockMap.get("block2"));
    }

    @Test
    public void assertAllowList() {
        ListsConf listsConf = getListsConf("allow-no-block.yml");
        assertNotNull(listsConf.getAllowLists());
        assertNotNull(listsConf.getBlockLists());

        Map<String, String> allowMap = listsConf.getAllowLists();
        Map<String, String> blockMap = listsConf.getBlockLists();

        assertFalse(allowMap.isEmpty());
        assertTrue(blockMap.isEmpty());

        assertEquals("url1", allowMap.get("allow1"));
        assertEquals("url2", allowMap.get("allow2"));
    }

    @Test
    public void assertBlockList() {
        ListsConf listsConf = getListsConf("block-no-allow.yml");
        assertNotNull(listsConf.getAllowLists());
        assertNotNull(listsConf.getBlockLists());

        Map<String, String> allowMap = listsConf.getAllowLists();
        Map<String, String> blockMap = listsConf.getBlockLists();

        assertTrue(allowMap.isEmpty());
        assertFalse(blockMap.isEmpty());

        assertEquals("url1", blockMap.get("block1"));
        assertEquals("url2", blockMap.get("block2"));
    }

}
