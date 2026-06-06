package com.allanvital.dnsao.infra.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public enum Log {

    DNS(LoggerFactory.getLogger("DNS")),
    CACHE(LoggerFactory.getLogger("CACHE")),
    INFRA(LoggerFactory.getLogger("INFRA"));

    private final Logger logger;

    Log(Logger logger) {
        this.logger = logger;
    }

    public void trace(String msg, Object... args) {
        logger.trace(msg, args);
    }

    public void debug(String msg, Object... args) {
        logger.debug(msg, args);
    }

    public void info(String msg, Object... args) {
        logger.info(msg, args);
    }

    public void warn(String msg, Object... args) {
        logger.warn(msg, args);
    }

    public void error(String msg, Object... args) {
        logger.error(msg, args);
    }

    public void error(String msg, Object arg, Throwable t) {
        logger.error(msg, arg, t);
    }

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

}
