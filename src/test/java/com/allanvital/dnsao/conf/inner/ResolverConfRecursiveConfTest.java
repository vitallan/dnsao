package com.allanvital.dnsao.conf.inner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ResolverConfRecursiveConfTest {

    @Test
    public void shouldExposeRecursiveConfByDefault() {
        ResolverConf resolverConf = new ResolverConf();

        assertNotNull(resolverConf.getRecursive());
        assertEquals(32, resolverConf.getRecursive().getMaxSteps());
        assertEquals(8, resolverConf.getRecursive().getMaxSubqueryDepth());
        assertEquals(3, resolverConf.getRecursive().getTimeoutSeconds());
        assertEquals(1000, resolverConf.getRecursive().getPerAuthorityTimeoutMillis());
    }
}
