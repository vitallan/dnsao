package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveConf {

    private int maxSteps = 32;
    private int maxSubqueryDepth = 8;
    private int maxRetries = 1;
    private int maxCnameRedirects = 8;
    private int timeoutSeconds = 3;
    private int perAuthorityTimeoutMillis = 1000;

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = Math.max(1, maxSteps);
    }

    public int getMaxSubqueryDepth() {
        return maxSubqueryDepth;
    }

    public void setMaxSubqueryDepth(int maxSubqueryDepth) {
        this.maxSubqueryDepth = Math.max(0, maxSubqueryDepth);
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    public int getMaxCnameRedirects() {
        return maxCnameRedirects;
    }

    public void setMaxCnameRedirects(int maxCnameRedirects) {
        this.maxCnameRedirects = Math.max(0, maxCnameRedirects);
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
    }

    public int getPerAuthorityTimeoutMillis() {
        return perAuthorityTimeoutMillis;
    }

    public void setPerAuthorityTimeoutMillis(int perAuthorityTimeoutMillis) {
        this.perAuthorityTimeoutMillis = Math.max(1, perAuthorityTimeoutMillis);
    }
}
