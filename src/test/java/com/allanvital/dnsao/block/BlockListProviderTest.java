package com.allanvital.dnsao.block;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockListProviderTest {

    private BlockListProvider blockListProvider;
    private TestFileHandler testFileHandler;

    @BeforeEach
    public void setup() {
        List<String> allowUrls = List.of("url1");
        List<String> blockUrls = List.of("url2");
        testFileHandler = new TestFileHandler();
        blockListProvider = new BlockListProvider(allowUrls, blockUrls, testFileHandler);
    }

    @Test
    public void shouldFillTheListsCorrectly() {
        Assertions.assertTrue(blockListProvider.getBlockList().contains("this.should.be.blocked"));
        Assertions.assertTrue(blockListProvider.getBlockList().contains("another.blocked.one"));
        Assertions.assertFalse(blockListProvider.getBlockList().contains("this.should.be.enabled"));
    }

}
