package com.allanvital.dnsao.block;

import com.allanvital.dnsao.utils.DownloadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.allanvital.dnsao.AppLoggers.INFRA;
import static com.allanvital.dnsao.utils.FileUtils.readFileEntries;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadFileHandler implements FileHandler {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final Path allowListPath;
    private final Path blockListPath;

    public DownloadFileHandler(Path rootPathToWorkOn) throws IOException {
        this.allowListPath = rootPathToWorkOn.resolve("allow");
        Files.createDirectories(allowListPath);
        this.blockListPath = rootPathToWorkOn.resolve("block");
        Files.createDirectories(blockListPath);
    }

    @Override
    public void downloadFiles(List<String> urls, ListType type) {
        Path toLookInto = pathToLookInto(type);
        List<String> fileNames = new LinkedList<>();
        if (urls != null && !urls.isEmpty()) {
            for (String url : urls) {
                String fileName = DownloadUtils.fileName(url);
                fileNames.add(fileName);
                try {
                    DownloadUtils.downloadToPath(url, toLookInto.resolve(fileName));
                } catch (IOException | InterruptedException e) {
                    log.warn("it was not possible to download {}. Error was {}", url, e.getMessage());
                }
            }
        }
        cleanOldFiles(toLookInto, fileNames);
    }

    @Override
    public Set<String> readAllEntriesOfType(ListType type) {
        Path toLookInto = pathToLookInto(type);
        Set<String> domains = new TreeSet<>();
        try (Stream<Path> list = Files.list(toLookInto)) {
            list.forEach(path -> {
                log.debug("reading entries from {}", path);
                domains.addAll(readFileEntries(path));
            });
        } catch (IOException e) {
            log.warn("it was not possible to read entries in file {}. Error was {}", toLookInto, e.getMessage());
        }
        return domains;
    }

    private Path pathToLookInto(ListType listType) {
        return (listType.equals(ListType.ALLOW)) ? (allowListPath) : (blockListPath);
    }

    private void cleanOldFiles(Path toClean, List<String> newFileNames) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(toClean)) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (!newFileNames.contains(fileName)) {
                    Files.deleteIfExists(entry);
                    log.debug("deleted file {}", fileName);
                }
            }
        } catch (IOException e) {
            log.warn("it was not possible to clean files on {}: {}", toClean, e.getMessage());
        }
    }

}
