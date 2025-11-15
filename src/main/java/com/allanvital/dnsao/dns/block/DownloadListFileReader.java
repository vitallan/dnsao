package com.allanvital.dnsao.dns.block;

import com.allanvital.dnsao.utils.DownloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;
import static com.allanvital.dnsao.utils.FileUtils.readFileEntries;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadListFileReader implements DomainListFileReader {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    @Override
    public Set<Long> readEntries(String url) {
        String fileName = DownloadUtils.fileName(url);
        try {
            Path downloadDir = DownloadUtils.getAppDir();
            Path finalPath = downloadDir.resolve(fileName);
            DownloadUtils.downloadToPath(url, finalPath);
            return readFileEntries(finalPath);
        } catch (IOException | InterruptedException e) {
            log.warn("it was not possible to download url {}. Error was {}", url, e.getMessage());
        }
        return Set.of();
    }

}
