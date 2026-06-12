package com.allanvital.dnsao.infra.dir;

import com.allanvital.dnsao.infra.dir.TempDirProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestTempDirProvider implements TempDirProvider {

    private final Path tempDirPath = Paths.get("data", "test-" + UUID.randomUUID());

    @Override
    public String getTempDir() {
        try {
            Files.createDirectories(tempDirPath);
        } catch (Exception e) {
            throw new RuntimeException("failed to create test temp dir " + tempDirPath, e);
        }
        return tempDirPath.toString();
    }

}
