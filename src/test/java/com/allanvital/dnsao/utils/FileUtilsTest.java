package com.allanvital.dnsao.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FileUtilsTest {

    @Test
    public void testDomainExtraction() {
        assertEquals(
                "domain.com",
                FileUtils.getDomainFromLine("0.0.0.0 domain.com"));

        assertEquals(
                "domain.com",
                FileUtils.getDomainFromLine("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff domain.com"));

        assertEquals(
                "domain.com",
                FileUtils.getDomainFromLine("0.0.0.0 domain.com #this is a comment"));

        assertEquals(
                "domain.com",
                FileUtils.getDomainFromLine("0.0.0.0    domain.com"));

        assertEquals(
                "",
                FileUtils.getDomainFromLine("#0.0.0.0    domain.com"));

        assertEquals(
                "domain.com",
                FileUtils.getDomainFromLine("domain.com"));
    }

}