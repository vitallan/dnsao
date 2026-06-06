package com.allanvital.dnsao.infra.log;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NamedLogRecord extends LogRecord {

    private final String threadName;

    public NamedLogRecord(Level level, String msg) {
        super(level, msg);
        this.threadName = simplifyThreadName(Thread.currentThread().getName());
    }

    public String getThreadName() {
        return threadName;
    }

    private static String simplifyThreadName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot <= 0) {
            return name;
        }
        int secondToLastDot = name.lastIndexOf('.', lastDot - 1);
        if (secondToLastDot > 0) {
            return name.substring(secondToLastDot + 1);
        }
        return name;
    }

}
