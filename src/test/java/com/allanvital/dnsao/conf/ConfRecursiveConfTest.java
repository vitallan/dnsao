package com.allanvital.dnsao.conf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ConfRecursiveConfTest {

    @Test
    public void shouldExposeRecursiveConfThroughConf() {
        Conf conf = new Conf();

        assertNotNull(conf.getResolver().getRecursive());
        assertEquals(32, conf.getResolver().getRecursive().getMaxSteps());
        assertEquals(8, conf.getResolver().getRecursive().getMaxSubqueryDepth());
        assertEquals(3, conf.getResolver().getRecursive().getTimeoutSeconds());
        assertEquals(1000, conf.getResolver().getRecursive().getPerAuthorityTimeoutMillis());
    }
}
