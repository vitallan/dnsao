package com.allanvital.dnsao.infra.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogFormatter extends Formatter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String levelName(Level level) {
        int lvl = level.intValue();
        if (lvl >= Level.SEVERE.intValue()) return "ERROR";
        if (lvl >= Level.WARNING.intValue()) return "WARN";
        if (lvl >= Level.INFO.intValue()) return "INFO";
        if (lvl >= Level.FINE.intValue()) return "DEBUG";
        return "TRACE";
    }

    private static String formatMessageWithRootLocale(LogRecord record) {
        String msg = record.getMessage();
        Object[] params = record.getParameters();
        if (params == null || params.length == 0) {
            return msg;
        }
        try {
            MessageFormat format = new MessageFormat(msg, Locale.ROOT);
            String[] stringArgs = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                stringArgs[i] = String.valueOf(params[i]);
            }
            return format.format(stringArgs);
        } catch (Exception e) {
            return msg;
        }
    }

    @Override
    public String format(LogRecord record) {
        String timestamp = DATE_FORMAT.format(new Date(record.getMillis()));
        String level = String.format("%-5s", levelName(record.getLevel()));
        String thread;
        if (record instanceof NamedLogRecord named) {
            thread = named.getThreadName();
        } else {
            thread = Thread.currentThread().getName();
        }
        String logger = record.getLoggerName();
        String message = formatMessageWithRootLocale(record);

        StringBuilder sb = new StringBuilder();
        sb.append('[').append(timestamp).append("] ");
        sb.append(level).append(" [");
        sb.append(thread).append("] [");
        sb.append(logger).append("] ");
        sb.append(message);
        sb.append(System.lineSeparator());

        Throwable thrown = record.getThrown();
        if (thrown != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            thrown.printStackTrace(pw);
            pw.flush();
            sb.append(sw);
        }

        return sb.toString();
    }

}
