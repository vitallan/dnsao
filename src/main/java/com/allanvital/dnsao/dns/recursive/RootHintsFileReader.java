package com.allanvital.dnsao.dns.recursive;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface RootHintsFileReader {

    Path readDownloaded(String url) throws IOException, InterruptedException;

    Optional<Path> readCached(String url) throws IOException;

    void persistCache(String url, Path source) throws IOException;
}
