package com.allanvital.dnsao.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadUtils {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    public static Path getAppDir() throws IOException {
        String base = System.getProperty("java.io.tmpdir");
        Path dir = Paths.get(base, "dns-files");
        Files.createDirectories(dir);
        return dir;
    }

    public static Path downloadToTemp(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        Path appDir = getAppDir();
        log.debug("downloading url {}", url);
        String fileName = fileName(uri);
        Path target = appDir.resolve(fileName);

        if (Files.exists(target)) {
            log.debug("cached file for {} already exists on {}", url, target);
            return target;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<Path> resp = client.send(
                req,
                HttpResponse.BodyHandlers.ofFile(target,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)
        );

        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP failure trying to download file " + url + " . Status: " + status);
        }

        log.debug("url {} downloaded at {}", url, target);

        return target;
    }

    public static String fileName(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            String auth = uri.getAuthority();
            host = (auth != null && !auth.isBlank()) ? auth : "unknown-host";
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        while (path.startsWith("/")) path = path.substring(1);
        while (path.endsWith("/")) path = path.substring(0, path.length() - 1);

        String name = path.isEmpty()
                ? host
                : host + "_" + path.replace('/', '_');

        name = name.replaceAll("[\\\\:*?\"<>|]", "_");
        name = name.replaceAll("__+", "_");

        return name;
    }



}