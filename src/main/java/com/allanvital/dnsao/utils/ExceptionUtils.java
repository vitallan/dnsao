package com.allanvital.dnsao.utils;
import com.allanvital.dnsao.infra.log.Log;



/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ExceptionUtils {


    public static Throwable findRootCause(Throwable throwable) {
        if (throwable == null) {
            return new Throwable("unknown original issue");
        }
        Log.INFRA.debug("finding root cause for: {}", throwable.getMessage(), throwable);
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

}