package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheConf {

    private boolean enabled = false;
    private int maxCacheEntries = 10000;
    private boolean rewarm = false;
    private int maxRewarmCount = 3;

    public boolean isRewarm() {
        return rewarm;
    }

    public void setRewarm(boolean rewarm) {
        this.rewarm = rewarm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    public void setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
    }

    public int getMaxRewarmCount() {
        return maxRewarmCount;
    }

    public void setMaxRewarmCount(int maxRewarmCount) {
        this.maxRewarmCount = maxRewarmCount;
    }
}