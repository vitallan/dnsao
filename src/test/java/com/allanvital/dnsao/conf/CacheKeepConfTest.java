package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.CacheConf;
import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class CacheKeepConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "cache";
    }

    private CacheConf cache(String file) {
        return getConf(file).getCache();
    }

    @Test
    public void keepDefault() {
        assertTrue(cache("cache-keep-default.yml").getKeep().isEmpty());
    }

    @Test
    public void keepWrongLevel() {
        List<String> keep = cache("cache-keep-wrong-level.yml").getKeep();
        assertNotNull(keep);
        assertTrue(keep.isEmpty());
    }

    @Test
    public void keepOneItem() {
        List<String> keep = cache("cache-keep-one-item.yml").getKeep();
        assertFalse(keep.isEmpty());
        assertTrue(keep.contains("url1.com"));
    }

    @Test
    public void keepMoreItens() {
        List<String> keep = cache("cache-keep-more-itens.yml").getKeep();
        assertFalse(keep.isEmpty());
        assertTrue(keep.contains("url1.com"));
        assertTrue(keep.contains("url2.com"));
        assertTrue(keep.contains("url3.com"));
        assertFalse(keep.contains("url4.com"));
    }

}
