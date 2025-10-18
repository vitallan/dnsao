package com.allanvital.dnsao.block;

import com.allanvital.dnsao.utils.FileUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestFileHandler implements FileHandler {

    private Map<ListType, Set<String>> lists = new HashMap<>();

    public TestFileHandler() {
        lists.put(ListType.BLOCK, getEntries("lists/blockList.txt"));
        lists.put(ListType.ALLOW, getEntries("lists/allowList.txt"));
    }

    private Set<String> getEntries(String file) {
        URL resource = getClass().getClassLoader().getResource(file);
        try {
            File f = new File(resource.toURI());
            return FileUtils.readFileEntries(f.toPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void downloadFiles(List<String> url, ListType type) {

    }

    @Override
    public Set<String> readAllEntriesOfType(ListType type) {
        return lists.get(type);
    }
}
