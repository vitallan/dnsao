package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.conf.inner.MiscConf;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServeExpiredConf extends ConfValidation {


    @Test
    public void expiredDifferentMax() {
        MiscConf miscConf = getConf("serve-expired-different-max.yml").getMisc();
        assertTrue(miscConf.isServeExpired());
        assertEquals(50, miscConf.getServeExpiredMax());
    }

    @Test
    public void disableExpired() {
        MiscConf miscConf = getConf("serve-expired-false.yml").getMisc();
        assertFalse(miscConf.isServeExpired());
    }

    @Test
    public void enableExpired() {
        MiscConf miscConf = getConf("serve-expired-true.yml").getMisc();
        assertTrue(miscConf.isServeExpired());
        assertEquals(1, miscConf.getServeExpiredMax());
    }

    @Override
    protected String getFolder() {
        return "serveExpired";
    }
}
