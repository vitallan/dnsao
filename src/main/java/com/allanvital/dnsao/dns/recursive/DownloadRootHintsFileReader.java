package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.utils.DownloadUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadRootHintsFileReader implements RootHintsFileReader {

    private static final String CACHE_FILE_PREFIX = "root-hints-";
    private static final String TEMP_FILE_PREFIX = "root-hints-download-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    @Override
    public Path readDownloaded(String url) throws IOException, InterruptedException {
        Path tempDir = DownloadUtils.getAppDir();
        Path tempFile = tempDir.resolve(TEMP_FILE_PREFIX + UUID.randomUUID() + TEMP_FILE_SUFFIX);
        return DownloadUtils.downloadToPath(url, tempFile);
    }

    @Override
    public Optional<Path> readCached(String url) throws IOException {
        Path cachedPath = cachePath(url);
        if (!Files.exists(cachedPath)) {
            return Optional.empty();
        }
        return Optional.of(cachedPath);
    }

    @Override
    public void persistCache(String url, Path source) throws IOException {
        Path cachedPath = cachePath(url);
        try {
            Files.move(source, cachedPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, cachedPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path cachePath(String url) throws IOException {
        Path appDir = DownloadUtils.getAppDir();
        return appDir.resolve(CACHE_FILE_PREFIX + DownloadUtils.fileName(url));
    }
}
