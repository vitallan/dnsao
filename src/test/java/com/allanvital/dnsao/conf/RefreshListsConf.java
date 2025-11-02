package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RefreshListsConf extends ConfValidation {

    private boolean getRefreshLists(String file) {
        return getConf(file).getMisc().isRefreshLists();
    }

    @Test
    public void refreshDefault() {
        assertFalse(getRefreshLists("refresh-lists-default.yml"));
    }

    @Test
    public void refreshTrue() {
        assertTrue(getRefreshLists("refresh-lists-true.yml"));
    }

    @Test
    public void refreshFalse() {
        assertFalse(getRefreshLists("refresh-lists-false.yml"));
    }

    @Override
    protected String getFolder() {
        return "refreshLists";
    }

}
