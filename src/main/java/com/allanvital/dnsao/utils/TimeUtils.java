package com.allanvital.dnsao.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TimeUtils {

    public static String formatMillis(long millis, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(formatter);
    }

    public static String formatMillisTime(long millis) {
        return formatMillis(millis, "HH:mm:ss.SSS");
    }

}
