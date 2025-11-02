package com.allanvital.dnsao.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ExceptionUtils {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    public static Throwable findRootCause(Throwable throwable) {
        if (log.isDebugEnabled()) {
            throwable.printStackTrace();
        }
        if (throwable == null) {
            return new Throwable("unknown original issue");
        }
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

}