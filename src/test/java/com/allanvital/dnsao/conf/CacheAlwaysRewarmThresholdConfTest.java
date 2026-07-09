package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.CacheConf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CacheAlwaysRewarmThresholdConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "cache";
    }

    private CacheConf cache(String file) {
        return getConf(file).getCache();
    }

    @Test
    public void alwaysRewarmTopEntriesDefaultsToZero() {
        assertEquals(0, cache("cache-always-rewarm-threshold-default.yml").getAlwaysRewarmTopEntries());
    }

    @Test
    public void alwaysRewarmTopEntriesLoadsExplicitValue() {
        assertEquals(3, cache("cache-always-rewarm-threshold-valid.yml").getAlwaysRewarmTopEntries());
    }

    @Test
    public void alwaysRewarmTopEntriesClampsNegativeValuesToZero() {
        assertEquals(0, cache("cache-always-rewarm-threshold-negative.yml").getAlwaysRewarmTopEntries());
    }

    @Test
    public void alwaysRewarmTopEntriesClampsToMaxCacheEntries() {
        assertEquals(8, cache("cache-always-rewarm-threshold-above-max.yml").getAlwaysRewarmTopEntries());
    }
}
