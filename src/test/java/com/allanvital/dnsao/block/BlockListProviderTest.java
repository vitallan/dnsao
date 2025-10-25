package com.allanvital.dnsao.block;

import com.allanvital.dnsao.utils.HashUtils;
import com.sun.source.tree.AssertTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        assertContains("this.should.be.blocked", true);
        assertContains("another.blocked.one", true);
        assertContains("this.should.be.enabled", false);

        assertTrue(blockListProvider.isBlocked(Name.fromString("this.should.be.blocked")));
        assertTrue(blockListProvider.isBlocked(Name.fromString("another.blocked.one")));
        assertTrue(blockListProvider.isBlocked(Name.fromString("www.another.blocked.one")));
        assertFalse(blockListProvider.isBlocked(Name.fromString("this.should.be.enabled")));
    }

    @Test
    public void shouldHandleAllowAndBlockListConsistently() throws TextParseException {
        assertContains("us-4.evergage.com", true);
        assertFalse(blockListProvider.isBlocked(Name.fromString("itauunibanco2.us-4.evergage.com")));
        assertFalse(blockListProvider.isBlocked(Name.fromString("itauunibanco2.us-4.evergage.com.")));
    }

    private void assertContains(String toContain, boolean shouldContain) {
        Long hash = HashUtils.fnv1a64(toContain);
        assertEquals(shouldContain, blockListProvider.getBlockList().contains(hash));
    }

}
