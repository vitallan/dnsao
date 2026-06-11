package com.allanvital.dnsao.component;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.fake.FakeRootHintsFileReader;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RootHintsStartupModeTest extends TestHolder {

    private FakeRootHintsFileReader rootHintsFileReader;

    @Override
    protected void setRootHints() throws ConfException {
    }

    @BeforeEach
    public void setup() {
        rootHintsFileReader = new FakeRootHintsFileReader();
        rootHintsFileReader.setDownloadedPath(validRootHintsPath());
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

    @Test
    public void recursiveStartupUsesConfiguredRootHintsUrl() throws Exception {
        loadConf("recursive-mode-stub.yml");
        conf.getMisc().setQueryLog(false);
        conf.getResolver().setRootHintsUrl("https://example.test/custom-root-hints");
        registerOverride(rootHintsFileReader);

        safeStartWithPresetConf();

        assertEquals(1, rootHintsFileReader.getReadDownloadedCalls());
        assertEquals(0, rootHintsFileReader.getReadCachedCalls());
        assertEquals("https://example.test/custom-root-hints", rootHintsFileReader.getLastDownloadedUrl());
    }

    @Test
    public void forwardStartupDoesNotTouchRootHintsReader() throws Exception {
        loadConf("forward-mode-stub.yml");
        conf.getMisc().setQueryLog(false);
        conf.getResolver().setRootHintsUrl("https://example.test/custom-root-hints");
        registerOverride(rootHintsFileReader);

        safeStartWithPresetConf();

        assertEquals(0, rootHintsFileReader.getReadDownloadedCalls());
        assertEquals(0, rootHintsFileReader.getReadCachedCalls());
    }

    private Path validRootHintsPath() {
        try {
            return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("rootHints/named.root.txt")).toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
