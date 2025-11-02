package com.allanvital.dnsao.conf.inner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class MiscConf {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private int timeout = 3;
    private boolean refreshLists = false;
    private DNSSecMode dnsSecMode = DNSSecMode.SIMPLE;
    private ExpiredConf expiredConf = new ExpiredConf();

    public String getDnssec() {
        return this.dnsSecMode.name();
    }

    public void setDnssec(String dnssec) {
        DNSSecMode secMode = DNSSecMode.SIMPLE;
        try {
            secMode = DNSSecMode.valueOf(dnssec.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("it was not possible to parse {}. Defaulting to SIMPLE", dnssec);
        }
        this.dnsSecMode = secMode;
    }

    public DNSSecMode getDnsSecMode() {
        return dnsSecMode;
    }

    public boolean isServeExpired() {
        return expiredConf.isServeExpired();
    }

    public void setServeExpired(boolean serveExpired) {
        this.expiredConf.setServeExpired(serveExpired);
    }

    public int getServeExpiredMax() {
        return this.expiredConf.getServeExpiredMax();
    }

    public void setServeExpiredMax(int serveExpiredMax) {
        this.expiredConf.setServeExpiredMax(serveExpiredMax);
    }

    public ExpiredConf getExpiredConf() {
        return this.expiredConf;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isRefreshLists() {
        return refreshLists;
    }

    public void setRefreshLists(boolean refreshLists) {
        this.refreshLists = refreshLists;
    }

}
