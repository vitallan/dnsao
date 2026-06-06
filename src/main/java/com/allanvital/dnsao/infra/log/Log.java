package com.allanvital.dnsao.infra.log;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public enum Log {

    DNS("DNS"),
    CACHE("CACHE"),
    INFRA("INFRA");

    @FunctionalInterface
    public interface Handler {
        void onEvent(Level level, String category, String message);
        Handler NOOP = (l, c, m) -> {};
    }

    private static final AtomicReference<Handler> handlerRef = new AtomicReference<>(Handler.NOOP);

    public static void setHandler(Handler h) {
        handlerRef.set(h != null ? h : Handler.NOOP);
    }

    private final Logger julLogger;
    private final String enumName;

    Log(String name) {
        this.enumName = name;
        this.julLogger = Logger.getLogger(name);
    }

    public void trace(String msg, Object... args) {
        log(Level.FINER, msg, args);
    }

    public void debug(String msg, Object... args) {
        log(Level.FINE, msg, args);
    }

    public void info(String msg, Object... args) {
        log(Level.INFO, msg, args);
    }

    public void warn(String msg, Object... args) {
        log(Level.WARNING, msg, args);
    }

    public void error(String msg, Object... args) {
        log(Level.SEVERE, msg, args);
    }

    public void error(String msg, Object arg, Throwable t) {
        if (!julLogger.isLoggable(Level.SEVERE)) {
            return;
        }
        String translated = translate(msg);
        Object[] params = arg != null ? new Object[]{arg} : new Object[0];
        String formatted = formatMessage(translated, params);
        handlerRef.get().onEvent(Level.SEVERE, enumName, formatted);
        NamedLogRecord record = new NamedLogRecord(Level.SEVERE, translated);
        record.setLoggerName(enumName);
        if (arg != null) {
            record.setParameters(params);
        }
        if (julLogger.isLoggable(Level.FINE)) {
            record.setThrown(t);
        }
        julLogger.log(record);
    }

    private void log(Level level, String msg, Object... args) {
        if (!julLogger.isLoggable(level)) {
            return;
        }
        Throwable thrown = null;
        Object[] remaining = args;
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Throwable t) {
            thrown = t;
            remaining = Arrays.copyOf(args, args.length - 1);
        }
        String translated = translate(msg);
        String formatted = formatMessage(translated, remaining);
        handlerRef.get().onEvent(level, enumName, formatted);
        NamedLogRecord record = new NamedLogRecord(level, translated);
        record.setLoggerName(enumName);
        if (remaining != null && remaining.length > 0) {
            record.setParameters(remaining);
        }
        if (thrown != null && julLogger.isLoggable(Level.FINE)) {
            record.setThrown(thrown);
        }
        julLogger.log(record);
    }

    static String translate(String msg) {
        if (msg == null) {
            return "";
        }
        if (!msg.contains("{}")) {
            return msg;
        }
        StringBuilder sb = new StringBuilder(msg.length() + 16);
        int argIndex = 0;
        int i = 0;
        while (i < msg.length()) {
            char c = msg.charAt(i);
            if (c == '\'') {
                sb.append("''");
                i++;
            } else if (c == '{' && i + 1 < msg.length() && msg.charAt(i + 1) == '}') {
                sb.append('{').append(argIndex++).append('}');
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String formatMessage(String translated, Object... args) {
        if (args == null || args.length == 0) {
            return translated.replace("''", "'");
        }
        try {
            MessageFormat format = new MessageFormat(translated, Locale.ROOT);
            String[] stringArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                stringArgs[i] = String.valueOf(args[i]);
            }
            return format.format(stringArgs);
        } catch (Exception e) {
            return translated.replace("''", "'");
        }
    }

}
