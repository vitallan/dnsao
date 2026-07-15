package com.allanvital.dnsao.conf.inner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveConfDefaultsTest {

    @Test
    public void shouldExposeSaneDefaults() {
        RecursiveConf recursiveConf = new RecursiveConf();

        assertEquals(32, recursiveConf.getMaxSteps());
        assertEquals(8, recursiveConf.getMaxSubqueryDepth());
        assertEquals(3, recursiveConf.getTimeoutSeconds());
        assertEquals(1000, recursiveConf.getPerAuthorityTimeoutMillis());
    }
}
