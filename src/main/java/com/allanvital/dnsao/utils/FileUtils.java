package com.allanvital.dnsao.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    public static final String COMMENT = "#";

    public static Set<String> readFileEntries(Path file) {
        Set<String> entries = new HashSet<>();
        int count = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String domain = getDomainFromLine(line);
                if (domain != null && !domain.isEmpty()) {
                    if (domain.endsWith(".")) {
                        domain = domain.substring(0, domain.length() - 1);
                    }
                    entries.add(domain.toLowerCase());
                    count++;
                }
            }
        } catch (IOException e) {
            Throwable rootCause = ExceptionUtils.findRootCause(e);
            log.error("failed to read " + file + " error was: " + rootCause);
        }
        log.debug("read a total of {} entries from file {}", count, file);
        return entries;
    }

    public static String getDomainFromLine(String rawLine) {
        String domain = "";
        String line = rawLine.trim();
        if (line.startsWith(COMMENT)) {
            return domain;
        }
        int hashPos = line.indexOf(COMMENT);
        if (hashPos >= 0) {
            line = line.substring(0, hashPos).trim();
        }
        if (line.isEmpty()) {
            return domain;
        }
        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return domain;
        }

        String first = parts[0];
        if (looksLikeIp(first)) {
            if (parts.length < 2) {
                return domain;
            }
            domain = parts[1];
        } else {
            domain = first;
        }
        return domain;
    }

    private static boolean looksLikeIp(String s) {
        return s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || s.contains(":");
    }

}