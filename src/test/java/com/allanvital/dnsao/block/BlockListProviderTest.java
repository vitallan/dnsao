package com.allanvital.dnsao.block;

import com.sun.source.tree.AssertTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void shouldFillTheListsCorrectly() throws TextParseException {
        assertTrue(blockListProvider.getBlockList().contains("this.should.be.blocked"));
        assertTrue(blockListProvider.getBlockList().contains("another.blocked.one"));
        assertFalse(blockListProvider.getBlockList().contains("this.should.be.enabled"));

        assertTrue(blockListProvider.isBlocked(Name.fromString("this.should.be.blocked")));
        assertTrue(blockListProvider.isBlocked(Name.fromString("another.blocked.one")));
        assertTrue(blockListProvider.isBlocked(Name.fromString("www.another.blocked.one")));
        assertFalse(blockListProvider.isBlocked(Name.fromString("this.should.be.enabled")));
    }

    @Test
    public void shouldHandleAllowAndBlockListConsistently() throws TextParseException {
        assertTrue(blockListProvider.getBlockList().contains("us-4.evergage.com"));
        assertFalse(blockListProvider.isBlocked(Name.fromString("itauunibanco2.us-4.evergage.com")));
    }

}
