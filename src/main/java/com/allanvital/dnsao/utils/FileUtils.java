package com.allanvital.dnsao.utils;
import com.allanvital.dnsao.infra.log.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FileUtils {


    public static final String COMMENT = "#";

    public static Set<Long> readFileEntries(Path file) {
        Set<Long> entries = new HashSet<>();
        int count = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String domain = getDomainFromLine(line);
                if (domain != null && !domain.isEmpty()) {
                    if (domain.endsWith(".")) {
                        domain = domain.substring(0, domain.length() - 1);
                    }
                    domain = domain.toLowerCase();
                    entries.add(HashUtils.fnv1a64(domain));
                    count++;
                }
            }
        } catch (IOException e) {
            Throwable rootCause = ExceptionUtils.findRootCause(e);
            Log.INFRA.error("failed to read {} error was: {}", file, rootCause);
        }
        Log.INFRA.debug("read a total of {} entries from file {}", count, file);
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