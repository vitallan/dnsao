package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.MiscConf;
import com.allanvital.dnsao.conf.inner.ResolverConf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TimeoutConfTest extends ConfValidation {

    @Override
    protected String getFolder() {
        return "timeout";
    }

    @Test
    public void disableExpired() {
        MiscConf miscConf = getConf("default-timeout.yml").getMisc();
        assertEquals(3, miscConf.getTimeout());
    }

    @Test
    public void enableExpired() {
        MiscConf miscConf = getConf("timeout-5.yml").getMisc();
        assertEquals(5, miscConf.getTimeout());
    }

}
