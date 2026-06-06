package com.allanvital.dnsao.utils;
import com.allanvital.dnsao.infra.log.Log;



/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ExceptionUtils {


    public static Throwable findRootCause(Throwable throwable) {
        if (Log.INFRA.isDebugEnabled()) {
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