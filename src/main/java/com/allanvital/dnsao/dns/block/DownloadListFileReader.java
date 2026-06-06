package com.allanvital.dnsao.dns.block;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.utils.DownloadUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static com.allanvital.dnsao.utils.FileUtils.readFileEntries;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadListFileReader implements DomainListFileReader {


    @Override
    public Set<Long> readEntries(String url) {
        String fileName = DownloadUtils.fileName(url);
        try {
            Path downloadDir = DownloadUtils.getAppDir();
            Path finalPath = downloadDir.resolve(fileName);
            DownloadUtils.downloadToPath(url, finalPath);
            return readFileEntries(finalPath);
        } catch (IOException | InterruptedException e) {
            Log.INFRA.warn("it was not possible to download url {}. Error was {}", url, e.getMessage());
        }
        return Set.of();
    }

}
