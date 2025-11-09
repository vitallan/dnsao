package com.allanvital.dnsao.conf.inner;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheConf {

    private boolean enabled = false;
    private int maxCacheEntries = 10000;
    private boolean rewarm = false;
    private int maxRewarmCount = 3;
    private int secsBeforeTtlToRewarm = 20;
    private List<String> keep = new LinkedList<>();

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

    public List<String> getKeep() {
        if (keep == null) {
            keep = new LinkedList<>();
        }
        return keep;
    }

    public void setKeep(List<String> keep) {
        this.keep = keep;
    }

    public int getSecsBeforeTtlToRewarm() {
        return secsBeforeTtlToRewarm;
    }

    public void setSecsBeforeTtlToRewarm(int secsBeforeTtlToRewarm) {
        this.secsBeforeTtlToRewarm = secsBeforeTtlToRewarm;
    }
}