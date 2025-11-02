package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ExpiredConf {

    private boolean serveExpired = false;
    private int serveExpiredMax = 86400; //1 day

    public boolean isServeExpired() {
        return serveExpired;
    }

    public void setServeExpired(boolean serveExpired) {
        this.serveExpired = serveExpired;
    }

    public int getServeExpiredMax() {
        return serveExpiredMax;
    }

    public void setServeExpiredMax(int serveExpiredMax) {
        this.serveExpiredMax = serveExpiredMax;
    }

}
