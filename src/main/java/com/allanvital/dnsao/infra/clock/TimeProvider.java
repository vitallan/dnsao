package com.allanvital.dnsao.infra.clock;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface TimeProvider {

    default long currentTimeInMillis() {
        return System.currentTimeMillis();
    }

}
