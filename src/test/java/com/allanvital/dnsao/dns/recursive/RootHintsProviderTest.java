package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.graph.fake.FakeRootHintsFileReader;
import com.allanvital.dnsao.infra.log.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RootHintsProviderTest {

    private static final String ROOT_HINTS_URL = "https://example.test/root-hints";

    @AfterEach
    public void resetLogHandler() {
        Log.setHandler(Log.Handler.NOOP);
    }

    @Test
    public void loadsDownloadedRootHintsAndParsesIpv4AndIpv6Glue() throws Exception {
        FakeRootHintsFileReader rootHintsFileReader = new FakeRootHintsFileReader();
        rootHintsFileReader.setDownloadedPath(resourcePath("rootHints/named.root.txt"));

        RootHintsProvider rootHintsProvider = new RootHintsProvider(ROOT_HINTS_URL, rootHintsFileReader);
        rootHintsProvider.initialize();

        List<String> ips = toIps(rootHintsProvider.getRootServers());
        assertEquals(26, ips.size());
        assertTrue(ips.contains("198.41.0.4"));
        assertTrue(ips.contains("2001:503:ba3e:0:0:0:2:30"));
        assertEquals(1, rootHintsFileReader.getReadDownloadedCalls());
        assertEquals(0, rootHintsFileReader.getReadCachedCalls());
        assertEquals(1, rootHintsFileReader.getPersistCacheCalls());
    }

    @Test
    public void fallsBackToCachedRootHintsWhenDownloadFails() throws Exception {
        FakeRootHintsFileReader rootHintsFileReader = new FakeRootHintsFileReader();
        rootHintsFileReader.setFailDownload(true);
        rootHintsFileReader.setCachedPath(resourcePath("rootHints/named.root.txt"));

        RootHintsProvider rootHintsProvider = new RootHintsProvider(ROOT_HINTS_URL, rootHintsFileReader);
        rootHintsProvider.initialize();

        List<String> ips = toIps(rootHintsProvider.getRootServers());
        assertEquals(26, ips.size());
        assertTrue(ips.contains("170.247.170.2"));
        assertEquals(1, rootHintsFileReader.getReadDownloadedCalls());
        assertEquals(1, rootHintsFileReader.getReadCachedCalls());
        assertEquals(0, rootHintsFileReader.getPersistCacheCalls());
    }

    @Test
    public void fallsBackToCachedRootHintsWhenDownloadedFileIsInvalid() throws Exception {
        FakeRootHintsFileReader rootHintsFileReader = new FakeRootHintsFileReader();
        rootHintsFileReader.setDownloadedPath(resourcePath("rootHints/invalid-no-glue.txt"));
        rootHintsFileReader.setCachedPath(resourcePath("rootHints/named.root.txt"));

        RootHintsProvider rootHintsProvider = new RootHintsProvider(ROOT_HINTS_URL, rootHintsFileReader);
        rootHintsProvider.initialize();

        List<String> ips = toIps(rootHintsProvider.getRootServers());
        assertEquals(26, ips.size());
        assertTrue(ips.contains("2001:dc3:0:0:0:0:0:35"));
        assertEquals(1, rootHintsFileReader.getReadDownloadedCalls());
        assertEquals(1, rootHintsFileReader.getReadCachedCalls());
        assertEquals(0, rootHintsFileReader.getPersistCacheCalls());
    }

    @Test
    public void fallsBackToEmbeddedBootstrapHintsWhenDownloadAndCacheFail() {
        FakeRootHintsFileReader rootHintsFileReader = new FakeRootHintsFileReader();
        rootHintsFileReader.setFailDownload(true);
        List<String> warnings = new ArrayList<>();
        Log.setHandler((level, category, message) -> {
            if ("INFRA".equals(category) && java.util.logging.Level.WARNING.equals(level)) {
                warnings.add(message);
            }
        });

        RootHintsProvider rootHintsProvider = new RootHintsProvider(ROOT_HINTS_URL, rootHintsFileReader);
        rootHintsProvider.initialize();

        List<String> ips = toIps(rootHintsProvider.getRootServers());
        assertEquals(13, ips.size());
        assertTrue(ips.contains("198.41.0.4"));
        assertTrue(warnings.stream().anyMatch(message -> message.contains("falling back to embedded bootstrap root hints")));
    }

    private Path resourcePath(String resourceName) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(resourceName)).toURI());
    }

    private List<String> toIps(List<NameServerAddress> rootServers) {
        return rootServers.stream().map(NameServerAddress::ip).collect(Collectors.toList());
    }
}
