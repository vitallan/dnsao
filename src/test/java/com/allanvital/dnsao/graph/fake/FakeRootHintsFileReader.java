package com.allanvital.dnsao.graph.fake;

import com.allanvital.dnsao.dns.recursive.RootHintsFileReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FakeRootHintsFileReader implements RootHintsFileReader {

    private Path downloadedPath;
    private Path cachedPath;
    private boolean failDownload;
    private int readDownloadedCalls;
    private int readCachedCalls;
    private int persistCacheCalls;
    private String lastDownloadedUrl;

    public void setDownloadedPath(Path downloadedPath) {
        this.downloadedPath = downloadedPath;
    }

    public void setCachedPath(Path cachedPath) {
        this.cachedPath = cachedPath;
    }

    public void setFailDownload(boolean failDownload) {
        this.failDownload = failDownload;
    }

    public int getReadDownloadedCalls() {
        return readDownloadedCalls;
    }

    public int getReadCachedCalls() {
        return readCachedCalls;
    }

    public int getPersistCacheCalls() {
        return persistCacheCalls;
    }

    public String getLastDownloadedUrl() {
        return lastDownloadedUrl;
    }

    @Override
    public Path readDownloaded(String url) throws IOException {
        readDownloadedCalls++;
        lastDownloadedUrl = url;
        if (failDownload) {
            throw new IOException("download failed");
        }
        if (downloadedPath == null) {
            throw new IOException("downloaded path was not configured");
        }
        return downloadedPath;
    }

    @Override
    public Optional<Path> readCached(String url) {
        readCachedCalls++;
        return Optional.ofNullable(cachedPath);
    }

    @Override
    public void persistCache(String url, Path source) {
        persistCacheCalls++;
        cachedPath = source;
    }
}
