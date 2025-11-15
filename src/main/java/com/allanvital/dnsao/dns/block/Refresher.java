package com.allanvital.dnsao.dns.block;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface Refresher {

    void scheduleRefresh(Runnable task);

}
