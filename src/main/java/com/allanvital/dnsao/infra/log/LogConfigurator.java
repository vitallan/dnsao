package com.allanvital.dnsao.infra.log;

import com.allanvital.dnsao.conf.inner.LogConf;
import com.allanvital.dnsao.conf.inner.LogFileConf;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogConfigurator {

    private static final String[] LOGGER_NAMES = {"DNS", "CACHE", "INFRA"};

    public static void reset() {
        for (String name : LOGGER_NAMES) {
            Logger logger = Logger.getLogger(name);
            for (Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
            }
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);
        }
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        root.setLevel(Level.OFF);
    }

    public static void configure(LogConf conf) {
        Handler fileHandler = null;
        LogFileConf fileConf = conf.getFile();
        if (fileConf != null && fileConf.getPath() != null && !fileConf.getPath().isEmpty()) {
            try {
                fileHandler = new AsyncFileHandler(
                    fileConf.getPath(),
                    fileConf.getMaxSize(),
                    fileConf.getMaxFiles(),
                    true
                );
                fileHandler.setLevel(Level.ALL);
            } catch (IOException e) {
                System.err.println("Could not create log file handler: " + e.getMessage());
            }
        }

        for (String name : LOGGER_NAMES) {
            Logger logger = Logger.getLogger(name);
            Level level = parseLevel(getLevel(conf, name));
            logger.setLevel(Level.ALL);
            logger.addHandler(new SystemOutHandler(new LogFormatter()));
            if (fileHandler != null) {
                logger.addHandler(fileHandler);
            }
            logger.setUseParentHandlers(false);
            Log.valueOf(name).setLevel(level);
        }

        Logger root = Logger.getLogger("");
        root.setLevel(parseLevel(conf.getRootLevel()));
        root.addHandler(new SystemOutHandler(new LogFormatter()));
        if (fileHandler != null) {
            root.addHandler(fileHandler);
        }
    }

    private static String getLevel(LogConf conf, String name) {
        return switch (name) {
            case "DNS" -> conf.getDns();
            case "CACHE" -> conf.getCache();
            case "INFRA" -> conf.getInfra();
            default -> "INFO";
        };
    }

    static Level parseLevel(String level) {
        if (level == null) return Level.INFO;
        return switch (level.toUpperCase()) {
            case "TRACE" -> Level.FINER;
            case "DEBUG" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARNING;
            case "ERROR" -> Level.SEVERE;
            case "OFF" -> Level.OFF;
            default -> Level.INFO;
        };
    }

}
